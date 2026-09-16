package com.tecnotree.eia.count.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "query")
public class CountQueryConfig {
	
	private String listOfQRecordsCount_ORCH;

	public String getListOfQRecordsCount_ORCH() {
		return listOfQRecordsCount_ORCH;
	}

	public void setListOfQRecordsCount_ORCH(String listOfQRecordsCount_ORCH) {
		this.listOfQRecordsCount_ORCH = listOfQRecordsCount_ORCH;
	}

	
	
}