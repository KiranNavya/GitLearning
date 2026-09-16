package com.tecnotree.eia.count.entity;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "order")
public class HwcbsOrderPayload {
	/*
	 * private Long orderId; private String status; private String productName;
	 * private String customerId;
	 * 
	 * // Getters and setters public Long getOrderId() { return orderId; }
	 * 
	 * public void setOrderId(Long orderId) { this.orderId = orderId; }
	 * 
	 * public String getStatus() { return status; }
	 * 
	 * public void setStatus(String status) { this.status = status; }
	 * 
	 * public String getProductName() { return productName; }
	 * 
	 * public void setProductName(String productName) { this.productName =
	 * productName; }
	 * 
	 * public String getCustomerId() { return customerId; }
	 * 
	 * public void setCustomerId(String customerId) { this.customerId = customerId;
	 * }
	 */
	private String orderId;
    private String orderDetail;
    @XmlElement
    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    @XmlElement
    public String getOrderDetail() {
        return orderDetail;
    }
    public void setOrderDetail(String orderDetail) {
        this.orderDetail = orderDetail;
    }
}