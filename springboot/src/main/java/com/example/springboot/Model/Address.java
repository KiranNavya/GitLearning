package com.example.springboot.Model;

import jakarta.xml.bind.annotation.XmlRootElement;

//@XmlRootElement
public class Address {
	private String village;
	private int post;
	
	
	public String getVillage() {
		return village;
	}
	public void setVillage(String village) {
		this.village = village;
	}
	public int getPost() {
		return post;
	}
	public void setPost(int post) {
		this.post = post;
	}
	
	

}
