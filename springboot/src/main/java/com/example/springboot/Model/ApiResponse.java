package com.example.springboot.Model;

import jakarta.xml.bind.annotation.XmlRootElement;


public class ApiResponse {
	private String Message;
	private Profile profile;
	
	
	public ApiResponse(String message, Profile profile) {
		super();
		Message = message;
		this.profile = profile;
	}
	
	
	public String getMessage() {
		return Message;
	}
	public void setMessage(String message) {
		Message = message;
	}
	public Profile getProfile() {
		return profile;
	}
	public void setProfile(Profile profile) {
		this.profile = profile;
	}
	
	
	
	
	
	
	

}
