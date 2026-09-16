package com.tecnotree.eia.count.beans;

public class ActionCodeConfig {
	private String actionCode;
//	private int executionPriority;
	private int totalRecords;

//	public ActionCodeConfig(String actionCode, int executionPriority, int totalRecords) {
	public ActionCodeConfig(String actionCode, int totalRecords) {
		this.actionCode = actionCode;
//		this.executionPriority = executionPriority;
		this.totalRecords = totalRecords;
	}

	public String getActionCode() {
		return actionCode;
	}

//	public int getExecutionPriority() {
//		return executionPriority;
//	}

	public int getTotalRecords() {
		return totalRecords;
	}
}