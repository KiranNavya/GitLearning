package com.tecnotree.eia.count.configuration;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "actions")
public class CountProperties {

	private Map<String, Integer> actionCodes;
	private Integer limitValue;

	public Map<String, Integer> getActionCodes() {
		return actionCodes;
	}

	public void setActionCodes(Map<String, Integer> actionCodes) {
		this.actionCodes = actionCodes;
	}

	public Integer getLimitValue() {
		return limitValue;
	}

	public void setLimitValue(Integer limitValue) {
		this.limitValue = limitValue;
	}
	
}
