package com.tecnotree.eia.commons.configuration;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "actions")
public class ActionCodeConfig {

    private Integer limitValue;
    private Map<String, Integer> actionCodes;

    public Integer getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(Integer limitValue) {
        this.limitValue = limitValue;
    }

    public Map<String, Integer> getActionCodes() {
        return actionCodes;
    }

    public void setActionCodes(Map<String, Integer> actionCodes) {
        this.actionCodes = actionCodes;
    }
}