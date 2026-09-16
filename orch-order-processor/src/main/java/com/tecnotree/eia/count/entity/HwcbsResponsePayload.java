package com.tecnotree.eia.count.entity;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response")
public class HwcbsResponsePayload {

	private String status; // Should be "SUCCESS" or "FAILURE" or similar
	private String message;

	@XmlElement
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@XmlElement
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	// Derived helper to simplify checks
	public boolean isSuccess() {
		return "SUCCESS".equalsIgnoreCase(this.status);
	}
}
