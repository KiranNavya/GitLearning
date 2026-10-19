package com.tecnotree.eia.processed.service;

import com.tecnotree.eia.EiaAuthentication;
import com.tecnotree.eia.commons.configuration.AuthenticationConfig;
import com.tecnotree.eia.commons.configuration.EndpointConfig;
import com.tecnotree.eia.commons.configuration.TokenProperties;
import com.tecnotree.eia.count.configuration.QueryConfig;
import com.tecnotree.eia.utility.ResponseConverterUtil;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SendToProcess {

    private static final Logger log = LoggerFactory.getLogger(SendToProcess.class);

    private final PrcExecutorService prcExecutorService;
    private RestTemplate restTemplate;
    private final EndpointConfig endpointConfig;
    private final QueryConfig queryConfig;
    private final AuthenticationConfig authenticationConfig;
    private final TokenProperties tokenProperties;
    private final Map<String, String> customHeaders;
    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${httpReadTimeout}")
    private int HttpReadTimeout;

    private EiaAuthentication eiaAuthentication = new EiaAuthentication();

    public SendToProcess(RestTemplateBuilder restTemplateBuilder, EndpointConfig endpointConfig, PrcExecutorService prcExecutorService, QueryConfig queryConfig, AuthenticationConfig authenticationConfig, TokenProperties tokenProperties, @Qualifier("customHeaders") Map<String, String> customHeaders) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.endpointConfig = endpointConfig;
        this.prcExecutorService = prcExecutorService;
        this.queryConfig = queryConfig;
        this.authenticationConfig = authenticationConfig;
        this.tokenProperties = tokenProperties;
        this.customHeaders = customHeaders;
    }

    @PostConstruct
    public void init() {
        this.restTemplate = restTemplateBuilder.connectTimeout(Duration.ofSeconds(60)).readTimeout(Duration.ofSeconds(HttpReadTimeout)).build();
        log.info("RestTemplate initialized with Read Timeout: {} seconds", HttpReadTimeout);
    }

    public void sendOrderToEia(Map<String, Object> order) {
        long startTime = System.currentTimeMillis();
        // Access values dynamically from the Map, assuming JDBC driver upper-cases column names
        Object orderId = order.get(queryConfig.getQueryMetaData().getPrimaryKeyColumn().toUpperCase());
        String actionCode = (String) order.get(queryConfig.getExtractedActionCodeColumn().toUpperCase());
        String requestPayload = (String) order.get(queryConfig.getQueryMetaData().getRequestPayloadColumn().toUpperCase());

        try {
            // Use dynamic actionCode to determine endpoint
            String endpointToUse = endpointConfig.getEndpointForAction(actionCode);

            if (endpointToUse == null) {
                log.error("No endpoint configured for action code: {}. Skipping order ID: {}", actionCode, orderId);
                return;
            }

            if (requestPayload == null || requestPayload.trim().isEmpty()) {
                log.error("Request Payload is null or empty for order ID {}. Skipping order.", orderId);
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            try {
                MediaType contentType = MediaType.parseMediaType(endpointConfig.getContentTypeHeader());
                headers.setContentType(contentType);
                log.info("Content-Type header set to '{}' for order ID {}", contentType, orderId);
            } catch (IllegalStateException e) {
                log.error("Invalid Content-Type configured: '{}'. Defaulting to text/xml. Error: {}", endpointConfig.getContentTypeHeader(), e.getMessage());
                headers.setContentType(MediaType.TEXT_XML);
            }
            if (endpointConfig.getSOAPAction() != null && !endpointConfig.getSOAPAction().trim().isEmpty()) {
                headers.set("SOAPAction", "\"" + endpointConfig.getSOAPAction() + "\"");
                log.info("SOAPAction header set to '{}' for order ID {}", endpointConfig.getSOAPAction(), orderId);
            } else {
                log.debug("SOAPAction header not configured or empty for order ID {}. Skipping SOAPAction header.", orderId);
            }

            // Add the condition for setting the AUTHORIZATION header
            if ("true".equalsIgnoreCase(authenticationConfig.getIsRequired())) {
                if (Boolean.TRUE.equals(tokenProperties.getIsTokenRequired())) {
                    String accessToken = getAccessTokenFromSSO();
                    headers.setBearerAuth(accessToken);
                    log.info("SSO token authentication enabled for order ID {}", orderId);
                } else {
                    String encryptedPassword;
                    if (authenticationConfig.getDynamic().isFlag()) {
                        final String dynamicPattern = "yyyyMMddHHmm";
                        String confDynamicSecretkey = authenticationConfig.getDynamic().getDynamicSecretKey();
                        String dynamicSecretKey = confDynamicSecretkey + DateTimeFormatter.ofPattern(dynamicPattern).format(LocalDateTime.now());

                        eiaAuthentication.setSecretKey(dynamicSecretKey);
                        encryptedPassword = eiaAuthentication.encrypt(authenticationConfig.getPassword());
                        log.info("Dynamic authentication enabled for order ID {}", orderId);
                    } else {
                        eiaAuthentication.setSecretKey(authenticationConfig.getStaticSecretKey());
                        encryptedPassword = eiaAuthentication.encrypt(authenticationConfig.getPassword());
                        log.info("Static authentication enabled for order ID {}", orderId);
                    }
                    String authStr = authenticationConfig.getUsername() + ":" + encryptedPassword;
                    String authValue = "Basic " + java.util.Base64.getEncoder().encodeToString(authStr.getBytes());
                    headers.set(HttpHeaders.AUTHORIZATION, authValue);
                    log.debug("Authorization header set for order ID {}", orderId);
                }
            } else {
                log.debug("Authorization header not required for order ID {}", orderId);
            }

            if (!customHeaders.isEmpty()) {
                customHeaders.forEach(headers::set);
                log.debug("Applied {} custom header(s) for order ID {}", customHeaders.size(), orderId);
            }

            HttpMethod httpMethod;
            try {
                httpMethod = HttpMethod.valueOf(endpointConfig.getHttpMethod().toUpperCase());
                log.info("Setting HTTP method:{} for orderId: {} ", httpMethod, orderId);
            } catch (IllegalArgumentException e) {
                log.error("Invalid HTTP method configured: '{}'. Defaulting to POST. Error: {}", endpointConfig.getHttpMethod(), e.getMessage());
                httpMethod = HttpMethod.POST;
            }

            log.info("Sending order ID {} to EIA ", orderId);
            HttpEntity<String> requestEntity = new HttpEntity<>(requestPayload, headers);
            ResponseEntity<String> response = restTemplate.exchange(endpointToUse, httpMethod, requestEntity, String.class);
            log.info("response from http call  {}  ", response.getBody());
            if (response.getBody() != null) {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Order {} sent successfully. Submitting PRC.", orderId);
                    if("application/json".equalsIgnoreCase(endpointConfig.getContentTypeHeader())) {
                   	 String xml = ResponseConverterUtil.extractStatusAsXml(response.getBody());
                        prcExecutorService.executeAsync(order, xml);
                   }else {
                	   if("application/json".equalsIgnoreCase(endpointConfig.getContentTypeHeader())) {
                         	 String xml = ResponseConverterUtil.extractStatusAsXml(response.getBody());
                              prcExecutorService.executeAsync(order, xml);
                         }else {
                          prcExecutorService.executeAsync(order, response.getBody());
                         }
                   }
                } else {
                    log.info("Order {} sent successfully. Submitting PRC with response of non 2xx ({}) status code.", orderId, response.getStatusCode());
                    prcExecutorService.executeAsync(order, response.getBody());
                }
            } else {
                log.warn("Order {} failed. HTTP Status: {} & null response body", orderId, response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            log.info("Failed to send order {} (Action: {}) to EIA due to HTTPClient error (Status: {}): {}", orderId, actionCode, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            log.error("Failed to send order {} (Action: {}) to EIA due to network/timeout issue: {}", orderId, actionCode, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send order {} to EIA: {}", orderId, e.getMessage(), e);
        } finally {
            long endTime = System.currentTimeMillis();
            log.debug("sendOrderToEia for order ID {} took {} ms", orderId, (endTime - startTime));
        }
    }

    private String getAccessTokenFromSSO() {
        String url = tokenProperties.getCLM_SSO_ENDPOINT();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.ALL));

        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        formParams.add("client_id", tokenProperties.getCLM_SSO_CLIENTID());
        formParams.add("client_secret", tokenProperties.getCLM_SSO_CLIENTSECRET());
        formParams.add("grant_type", tokenProperties.getGRANT_TYPE());
        formParams.add("username", tokenProperties.getCLM_SSO_USERNAME());
        formParams.add("password", tokenProperties.getCLM_SSO_PASSWORD());
        formParams.add("scope", tokenProperties.getSCOPE());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formParams, headers);

        try {
            log.info("Requesting SSO token from URL: {}", url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object token = response.getBody().get("access_token");
                if (token instanceof String accessToken && !accessToken.isBlank()) {
                    return accessToken;
                }
                throw new RuntimeException("Access token missing in SSO response");
            }

            throw new RuntimeException("Failed to retrieve SSO token: " + response.getStatusCode());
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("SSO token request failed: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new RuntimeException("SSO token request failed", ex);
        } catch (Exception ex) {
            log.error("Exception while fetching SSO token", ex);
            throw new RuntimeException("Exception while fetching SSO token", ex);
        }
    }
}
