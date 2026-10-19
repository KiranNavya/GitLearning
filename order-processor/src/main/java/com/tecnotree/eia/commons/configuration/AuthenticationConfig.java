package com.tecnotree.eia.commons.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Base64;

@Component
@ConfigurationProperties(prefix = "authentication")
public class AuthenticationConfig {
    private String isRequired;
    private DynamicConfig dynamic;
    private String staticSecretKey;
    private String username;
    private String password;

    public String getStaticSecretKey() {
        return staticSecretKey;
    }

    public void setStaticSecretKey(String staticSecretKey) {
        this.staticSecretKey = staticSecretKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        if (this.password == null || this.password.isEmpty()) {
            return this.password; // Return null or empty if already so
        }
        try {
            return new String(Base64.getDecoder().decode(this.password));
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Failed to Base64 decode password. Returning original value. Error: " + e.getMessage());
            return this.password; // Return original value if decoding fails
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(String isRequired) {
        this.isRequired = isRequired;
    }

    public DynamicConfig getDynamic() {
        return dynamic;
    }

    public void setDynamic(DynamicConfig dynamic) {
        this.dynamic = dynamic;
    }

    public static class DynamicConfig {
        private boolean flag;
        private String dynamicSecretKey;

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        public String getDynamicSecretKey() {
            return dynamicSecretKey;
        }

        public void setDynamicSecretKey(String dynamicSecretKey) {
            this.dynamicSecretKey = dynamicSecretKey;
        }
    }
}
