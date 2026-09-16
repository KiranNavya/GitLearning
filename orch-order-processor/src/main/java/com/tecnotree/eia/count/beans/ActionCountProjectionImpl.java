package com.tecnotree.eia.count.beans;

public class ActionCountProjectionImpl implements ActionCountProjection {

	private String actionCode;
	private Integer recordCount;
	
	public ActionCountProjectionImpl() {
		super();
	}
	
	public ActionCountProjectionImpl(String actionCode, Integer recordCount) {
		super();
		this.actionCode = actionCode;
		this.recordCount = recordCount;
	}
	
	/*
	 * public ActionCountProjectionImpl(String actionCode, Integer recordCount) {
	 * super(); this.actionCode = actionCode; this.recordCount = recordCount; }
	 */

	@Override
	public String getActionCode() {
		// TODO Auto-generated method stub
		return actionCode;
	}

	@Override
	public Integer getRecordCount() {
		// TODO Auto-generated method stub
		return recordCount;
	}

}
