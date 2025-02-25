package com.example.springboot.Model;
import jakarta.xml.bind.annotation.XmlRootElement;

//@XmlRootElement
public class Profile {
	 private String name;
	 private String lastName;
	 private Address address;
	
	 
	 
	 public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}
	 
	 
	 

}
