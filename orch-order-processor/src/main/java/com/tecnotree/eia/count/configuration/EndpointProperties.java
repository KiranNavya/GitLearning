package com.tecnotree.eia.count.configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "endpoint")
public class EndpointProperties {
	
	private Map<String, String> url;

    public Map<String, String> getUrl() {
        return url;
    }

    public void setUrl(Map<String, String> url) {
        this.url = url;
    }

    public String getUrlByActionCode(String actionCode) {
        return url.get(actionCode);
    }

}
