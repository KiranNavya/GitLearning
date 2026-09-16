package com.tecnotree.eia.count.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "token")
public class TokenProperties {
	
	private String CLM_SSO_CLIENTID;
	private String CLM_SSO_CLIENTSECRET;
	private String CLM_SSO_USERNAME;
	private String CLM_SSO_PASSWORD;
	private String CLM_SSO_ENDPOINT;
	private String GRANT_TYPE;
	private String SCOPE;
	private Boolean IsTokenRequired;
	
	public String getCLM_SSO_CLIENTID() {
		return CLM_SSO_CLIENTID;
	}
	public void setCLM_SSO_CLIENTID(String cLM_SSO_CLIENTID) {
		CLM_SSO_CLIENTID = cLM_SSO_CLIENTID;
	}
	public String getCLM_SSO_CLIENTSECRET() {
		return CLM_SSO_CLIENTSECRET;
	}
	public void setCLM_SSO_CLIENTSECRET(String cLM_SSO_CLIENTSECRET) {
		CLM_SSO_CLIENTSECRET = cLM_SSO_CLIENTSECRET;
	}
	public String getCLM_SSO_USERNAME() {
		return CLM_SSO_USERNAME;
	}
	public void setCLM_SSO_USERNAME(String cLM_SSO_USERNAME) {
		CLM_SSO_USERNAME = cLM_SSO_USERNAME;
	}
	public String getCLM_SSO_PASSWORD() {
		return CLM_SSO_PASSWORD;
	}
	public void setCLM_SSO_PASSWORD(String cLM_SSO_PASSWORD) {
		CLM_SSO_PASSWORD = cLM_SSO_PASSWORD;
	}
	public String getCLM_SSO_ENDPOINT() {
		return CLM_SSO_ENDPOINT;
	}
	public void setCLM_SSO_ENDPOINT(String cLM_SSO_ENDPOINT) {
		CLM_SSO_ENDPOINT = cLM_SSO_ENDPOINT;
	}
	public String getGRANT_TYPE() {
		return GRANT_TYPE;
	}
	public void setGRANT_TYPE(String gRANT_TYPE) {
		GRANT_TYPE = gRANT_TYPE;
	}
	public String getSCOPE() {
		return SCOPE;
	}
	public void setSCOPE(String sCOPE) {
		SCOPE = sCOPE;
	}
	public Boolean getIsTokenRequired() {
		return IsTokenRequired;
	}
	public void setIsTokenRequired(Boolean isTokenRequired) {
		IsTokenRequired = isTokenRequired;
	}
	
	
	
	
	
	

}
