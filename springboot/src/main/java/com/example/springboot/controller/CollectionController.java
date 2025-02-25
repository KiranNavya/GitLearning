package com.example.springboot.controller;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.helpers.Reporter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.PostExchange;

import com.example.springboot.Model.ApiResponse;
import com.example.springboot.Model.Profile;
import com.example.springboot.Model.UserRequest;

import java.util.stream.Collectors;

@RestController // Used For Restfull Web Sevices where Controller and ResponseBody together called RestController
//@RequestMapping("/Receivedxml")
public class CollectionController {
	
	@RequestMapping("/") 
	public ResponseEntity<String> Responseentity() {  //ResponseEntity use to return status , header,Body
		System.out.print("Enterning");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("EnterningBoss");
	}
	
	@RequestMapping("/collect")
	public ResponseEntity<String> collecting(@RequestBody UserRequest Request) {
		List<Profile> profile = Request.getProfile();
		return ResponseEntity.ok("Received profile ::" + profile.get(0).getLastName() +" "+profile.get(0).getLastName()+" "+profile.get(0).getAddress().getVillage());
	}
	
	@RequestMapping("/Received2")
	public ResponseEntity<ApiResponse> Recieved(@RequestBody UserRequest Request) {
		ApiResponse response = new ApiResponse("user data Successfully Recieved" ,Request.getProfile().get(0));
		return ResponseEntity.ok(response);
	}

	@PostMapping(value = "/user", consumes = MediaType.APPLICATION_XML_VALUE,produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<ApiResponse> Recievedxml(@RequestBody UserRequest Request) {
		Request.getProfile().get(0).setLastName(Request.getProfile().get(0).getLastName().toUpperCase());
		ApiResponse response = new ApiResponse("user data Successfully Recieved" ,Request.getProfile().get(0));
		return ResponseEntity.ok(response);
	}
	
	@RequestMapping("/Received")
	public ResponseEntity<Map<String,Object>> storeinMap(@RequestBody UserRequest Request) {
		Map<String,Object> response = new HashMap<>();
		    response.put("message", "user data Successfully Received");
		    response.put("profile", Request.getProfile());
		    
		    Map<String,Object> additionalDetails = new HashMap<>();
		    additionalDetails.put("gender", "Male");
		    additionalDetails.put("age", "Female");
		    
		    Map<String,Object> SecondaryDetails = new HashMap<>();
		    SecondaryDetails.put("nickName", "kiran");
		    additionalDetails.put("SecondaryDetails", SecondaryDetails);
		   
		    response.put("additionalDetails",additionalDetails);
		return ResponseEntity.ok(response);
	}
	
	@RequestMapping("/ReceivedStreams")
	public ResponseEntity<Map<String,Object>> storeinMapStreams(@RequestBody UserRequest Request) {
		Map<String,Object> response = new HashMap<>();
		    response.put("message", "user data Successfully Received");
		    
		    
		    List<Profile> filteredProfiles = Request.getProfile()
	                .stream()  // ✅ stream() is applied on List<Profile>
	                .filter(profile -> "kiarn".equalsIgnoreCase(profile.getName()) && "Vanarasi".equalsIgnoreCase(profile.getAddress().getVillage())) // Filtering condition
	                .collect(Collectors.toList());;
	                
	                
	         response.put("profile", filteredProfiles);         
		    
		    Map<String,Object> additionalDetails = new HashMap<>();
		    additionalDetails.put("gender", "Male");
		    additionalDetails.put("age", "Female");
		    
		    Map<String,Object> SecondaryDetails = new HashMap<>();
		    SecondaryDetails.put("nickName", "kiran");
		    additionalDetails.put("SecondaryDetails", SecondaryDetails);
		   
		    response.put("additionalDetails",additionalDetails);
		return ResponseEntity.ok(response);
	}
	
	@RequestMapping("/List")
	public List<String> collectionList(@RequestBody UserRequest Request) {
		List<Profile> profile = Request.getProfile();
		List<String> list = new ArrayList<>();
		Iterator<Profile> iterator = profile.iterator();
		while(iterator.hasNext()) {
			list.add(iterator.next().getName());
		}
		
	Iterator<String> listIterator=list.iterator();
	while(listIterator.hasNext()) {
		System.out.println(listIterator.next());
	}
		
		return list;
	}
}
