package com.tecnotree.eia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class OrderProcessorApplication {
	
	private static final Logger log = LoggerFactory.getLogger(OrderProcessorApplication.class);

	public static void main(String[] args) {
		log.info("Starting Order Processor Application...");
		SpringApplication.run(OrderProcessorApplication.class, args);
		log.info("Order Processor Application started successfully.");		
	}	
}