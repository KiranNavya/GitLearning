package com.tecnotree.eia.commons.configuration;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "endpoints")
public class EndpointConfig {
    private String httpMethod;
    private String contentTypeHeader;
    private String SOAPAction;
    private Map<String, String> url;

    public Map<String, String> getUrl() {
        return url;
    }

    public void setUrl(Map<String, String> url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getContentTypeHeader() {
        return contentTypeHeader;
    }

    public void setContentTypeHeader(String contentTypeHeader) {
        this.contentTypeHeader = contentTypeHeader;
    }

    public String getSOAPAction() {
        return SOAPAction;
    }

    public void setSOAPAction(String SOAPAction) {
        this.SOAPAction = SOAPAction;
    }

    public String getEndpointForAction(String actionCode) {
        String defaultEndpoint = url.get("eiaProxy");
        return url.getOrDefault(actionCode, defaultEndpoint);
    }
}