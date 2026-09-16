package com.tecnotree.eia.processed.service;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.tecnotree.eia.count.entity.NotificationPayload;

@Component
public class Notifier {
	private final RestTemplate restTemplate = new RestTemplate();
    private final String clmEndpoint = "http://clm-system/api/order-status"; // Replace with actual URL

    public void notifySystem(NotificationPayload payload) {
        try {
            restTemplate.postForEntity(clmEndpoint, payload, Void.class);
            System.out.println("Notification sent to CLM for Order ID: " + payload.getOrderId());
        } catch (Exception e) {
            System.err.println("Failed to notify CLM for Order ID: " + payload.getOrderId());
            e.printStackTrace();
        }
    }
}