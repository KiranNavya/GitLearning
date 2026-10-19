package com.tecnotree.eia.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

//@SuppressWarnings("unchecked")
public class ResponseConverterUtil {

//    public static String convertJsonToXml(String jsonString) {
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            Map<String, Object> map = mapper.readValue(jsonString, Map.class);
//
//            StringBuilder xml = new StringBuilder();
//            xml.append("<root>");
//
//            for (Map.Entry<String, Object> entry : map.entrySet()) {
//                Object value = entry.getValue();
//
//                xml.append("<").append(entry.getKey()).append(">")
//                   .append(escapeXml(value))
//                   .append("</").append(entry.getKey()).append(">");
//            }
//
//            xml.append("</root>");
//            return xml.toString();
//
//        } catch (Exception e) {
//            throw new RuntimeException("Error converting JSON to XML", e);
//        }
//    }
//
//    private static String escapeXml(Object value) {
//        if (value == null) return "";
//        return value.toString()
//                .replace("&", "&amp;")
//                .replace("<", "&lt;")
//                .replace(">", "&gt;");
//    }
	
//	public static String extractStatusAsXml(String jsonString) {
//	    try {
//	        if (jsonString == null || jsonString.isEmpty()) {
//	            return "<ROOT><status>Null Response</status></ROOT>";
//	        }
//
//	        JSONObject responseJsonObject = new JSONObject(jsonString);
//	        String status = "UNKNOWN";
//
//	        if (responseJsonObject.has("steps")) {
//	            JSONArray steps = responseJsonObject.getJSONArray("steps");
//
//	            for (int i = 0; i < steps.length(); i++) {
//	                JSONObject step = steps.getJSONObject(i);
//
//	                if ("End".equalsIgnoreCase(step.optString("action"))) {
//	                    JSONObject requestPayload = step.optJSONObject("requestPayload");
//
//	                    if (requestPayload != null) {
//	                        status = requestPayload.optString("status", "UNKNOWN");
//	                    }
//	                }
//	            }
//	        }
//
//	        return "<ROOT><status>" + status + "</status></ROOT>";
//
//	    } catch (Exception e) {
//	        return "<ROOT><status>ERROR</status></ROOT>";
//	    }
//	}
	
	public static String extractStatusAsXml(String jsonString) {
	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        JsonNode root = mapper.readTree(jsonString);

	        String status = "UNKNOWN";

	        JsonNode steps = root.path("steps");

	        if (steps.isArray()) {
	            for (JsonNode step : steps) {
	                if ("End".equalsIgnoreCase(step.path("action").asText())) {
	                    status = step.path("requestPayload")
	                                 .path("status")
	                                 .asText("UNKNOWN");
	                }
	            }
	        }

	        return "<ROOT><status>" + status + "</status></ROOT>";

	    } catch (Exception e) {
	        return "<ROOT><status>ERROR</status></ROOT>";
	    }
	}
	
	
}
