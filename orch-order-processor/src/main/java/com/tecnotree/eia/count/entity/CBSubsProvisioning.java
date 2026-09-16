package com.tecnotree.eia.count.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "CB_ORCH_REQUEST_DTLS")
@IdClass(CBSubsProvisioningId.class)
public class CBSubsProvisioning {
	@Id
	@Column(name = "SCHDL_LINK_CODE_N")
	private Long orderId;
	
	@Column(name="SEQ_NUM_N")
	private String sequenceNumber;

	@Column(name = "STATUS_CODE_V")
	private String status;

	@Column(name = "REQUEST_OBJECT")
	private String request;
	
	@Column(name = "ACTION_CODE_V")
    private String actionCode;
	
	@Column(name = "URL_V")
	private String endpoint;
	
	@Column(name = "OPERATION_NAME_V")
	private String operationName;
	
	@Column(name = "RESPONSE_OBJECT")
	private String response_body;
	
	@Column(name = "PAYMENT_MODE_V")
	private String paymentmode;

	public String getPaymentmode() {
		return paymentmode;
	}

	public void setPaymentmode(String paymentmode) {
		this.paymentmode = paymentmode;
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}


	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getActionCode() {
		return actionCode;
	}

	public void setActionCode(String actionCode) {
		this.actionCode = actionCode;
	}

	public String getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(String sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public String getOperationName() {
		return operationName;
	}

	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	public String getResponse_body() {
		return response_body;
	}

	public void setResponse_body(String response) {
		this.response_body = response;
	}
	
	
	
	
	
	
	
}
