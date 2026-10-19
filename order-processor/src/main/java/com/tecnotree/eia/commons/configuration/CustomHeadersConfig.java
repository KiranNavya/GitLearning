package com.tecnotree.eia.commons.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CustomHeadersConfig {

    @Bean("customHeaders")
    @ConfigurationProperties(prefix = "custom-headers")
    public Map<String, String> customHeaders() {
        return new HashMap<>();
    }
}
