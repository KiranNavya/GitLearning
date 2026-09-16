package com.tecnotree.eia.processed.service;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.tecnotree.eia.OrderProcessorApplication;
import com.tecnotree.eia.count.configuration.EndpointProperties;
import com.tecnotree.eia.count.configuration.TokenProperties;

import com.tecnotree.eia.count.entity.CBSubsProvisioning;
import com.tecnotree.eia.count.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;


@Component
public class SendToProcess {
    private static final Logger log = LoggerFactory.getLogger(OrderProcessorApplication.class);

    private final RestTemplate restTemplate;

    @Autowired
    private TokenProperties tokenProperties;

    @Autowired
    private EndpointProperties endpointProperties;

    @Autowired
    private OrderRepository orderRepo;

    public SendToProcess(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public void sendOrderToCLM(CBSubsProvisioning order) {
        try {
            String token = null;
            String endpointUrl = null;
            String payload = order.getRequest();

            if (payload == null || payload.trim().isEmpty()) {
                throw new IllegalArgumentException("Empty or null payload for order " + order.getOrderId());
            }

            payload = payload.trim();

            if (tokenProperties.getIsTokenRequired()) {
                token = getAccessTokenFromSSO();
            }

            HttpHeaders headers = new HttpHeaders();
            MediaType contentType;

            char firstChar = payload.charAt(0);
            if (firstChar == '<') {
                contentType = MediaType.APPLICATION_XML;
            } else if (firstChar == '{' || firstChar == '[') {
                contentType = MediaType.APPLICATION_JSON;
            } else {
                contentType = detectContentTypeByParsing(payload);
            }

            headers.setContentType(contentType);
            if (token != null) {
                headers.setBearerAuth(token);
            } else {
                headers.add("SSO_TOKEN_REQUIRED", tokenProperties.getIsTokenRequired() ? "Y" : "N");
            }
            if("D".equalsIgnoreCase(order.getPaymentmode())) {
            	 endpointUrl = endpointProperties.getUrlByActionCode(order.getActionCode()+"_D");

            	 if (endpointUrl == null) {
            	        endpointUrl = endpointProperties.getUrlByActionCode(order.getActionCode());
            	    }

            	
            }else {
            endpointUrl = endpointProperties.getUrlByActionCode(order.getActionCode());
            }
            
            if (endpointUrl == null) {
                endpointUrl = endpointProperties.getUrlByActionCode("default");
                //throw new IllegalArgumentException("No endpoint URL configured for action code: " + order.getActionCode());
            }

            log.info("Sending request to CLM. Token required: {}", tokenProperties.getIsTokenRequired());
            log.debug("Request Payload: {}", payload);
            log.debug("Request Headers: {}", headers);
            log.debug("Request Endpoint: {}", endpointUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(endpointUrl, new HttpEntity<>(payload, headers), String.class);

            new Thread(() -> {
                try {
                    log.info("Response Status: {}", response.getStatusCode());
                    String responseBody = response.getBody();
                    log.debug("Response Body: {}", responseBody);

                    boolean success = false;
                    String responseToSave = responseBody;

                    if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                        if (MediaType.APPLICATION_JSON.equals(contentType)) {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode root = mapper.readTree(responseBody);
                            JsonNode responseNode = root.path("response");
                            success = responseNode.has("body");

                            try {
                                XmlMapper xmlMapper = new XmlMapper();
                                String xml = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                                responseToSave = xml;
                                xmlMapper.writeValue(new File("order_" + order.getOrderId() + ".xml"), root);
                                log.info("Saved JSON response as XML for order {}", order.getOrderId());
                            } catch (Exception ex) {
                                log.error("Failed to convert JSON to XML for order {}: {}", order.getOrderId(), ex.getMessage());
                            }

                        } else if (MediaType.APPLICATION_XML.equals(contentType)) {
                            if (responseBody.contains("<body>") || responseBody.contains("<Body>") || responseBody.contains("<status>RECEIVED</status>")) {
                                success = true;
                            } else {
                                log.warn("Unexpected XML response format for order {}: {}", order.getOrderId(), responseBody);
                            }
                        }
                    } else {
                        log.warn("Order {} - HTTP call failed with status: {}", order.getOrderId(), response.getStatusCode());
                    }

                    order.setStatus(success ? "E" : "R");
                    order.setResponse_body(responseToSave);
                    orderRepo.save(order);
                    log.info("Order {} updated with status {}.", order.getOrderId(), order.getStatus());

                } catch (Exception e) {
                    order.setStatus("R");
                    orderRepo.save(order);
                    log.error("Exception during response handling for order {}: {}", order.getOrderId(), e.getMessage(), e);
                }
            }).start();

        } catch (IllegalArgumentException e) {
            log.error("Invalid input for order {}: {}", order.getOrderId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send order {} to CLM: {}", order.getOrderId(), e.getMessage(), e);
            order.setStatus("R");
            orderRepo.save(order);
        }
    }


    private MediaType detectContentTypeByParsing(String payload) {
        ObjectMapper jsonMapper = new ObjectMapper();
        XmlMapper xmlMapper = new XmlMapper();

        try {
            jsonMapper.readTree(payload);
            return MediaType.APPLICATION_JSON;
        } catch (Exception ignored) {
        }

        try {
            xmlMapper.readTree(payload.getBytes());
            return MediaType.APPLICATION_XML;
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("Unsupported request format");
    }


    private String createFormatErrorXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<error>\n" + "    <message>Response format is wrong</message>\n" + "</error>";
    }



/*	private String extractResultCode(String xml) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

			NodeList resultNodes = doc.getElementsByTagNameNS("*", "ResultCode");
			if (resultNodes.getLength() == 0) {
				resultNodes = doc.getElementsByTagNameNS("*", "resultCode");
			}
			if (resultNodes.getLength() == 0) {
				resultNodes = doc.getElementsByTagNameNS("*", "Resultcode");
			}

			if (resultNodes.getLength() > 0) {
				return resultNodes.item(0).getTextContent().trim();
			}

		} catch (Exception e) {
			log.error("Error parsing ResultCode from XML: {}", e.getMessage());
		}
		return null;
	}  */

    public String getAccessTokenFromSSO() {
        String url = tokenProperties.getCLM_SSO_ENDPOINT();  // e.g., https://192.168.20.40:9443/oauth2/token

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

            log.info("Token_Response Status: {}", response.getStatusCode());
            log.debug("Token_Response Body: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object tokenObj = response.getBody().get("access_token");
                if (tokenObj instanceof String token && !token.isBlank()) {
                    return token;
                } else {
                    throw new RuntimeException("Access token missing or invalid in response");
                }
            } else {
                throw new RuntimeException("Failed to retrieve token: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("SSO Token Request Failed: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new RuntimeException("SSO Token Request Failed: " + ex.getMessage(), ex);
        } catch (Exception e) {
            log.error("Exception occurred while fetching SSO token", e);
            throw new RuntimeException("Exception occurred while fetching SSO token", e);
        }
    }


}
