package org.flowvisor.api;

import java.util.HashMap;

public class DeviceAdvertisement {
	long dpid;
	// from ofp_stats_desc()
	String mfr_desc;  
	String hw_desc;
	String sw_desc;
	String serial_num;
	String dp_desc;

	// list of key=value pairs, for extensibility
	HashMap<String,String> attributes;

	protected DeviceAdvertisement() {
		// do nothing, for java beans
	}
	
	public DeviceAdvertisement(long dpid, String mfrDesc, String hwDesc,
			String swDesc, String serialNum, String dpDesc) {
		super();
		this.dpid = dpid;
		mfr_desc = mfrDesc;
		hw_desc = hwDesc;
		sw_desc = swDesc;
		serial_num = serialNum;
		dp_desc = dpDesc;
	}

	public long getDpid() {
		return dpid;
	}

	public void setDpid(long dpid) {
		this.dpid = dpid;
	}

	public String getMfr_desc() {
		return mfr_desc;
	}

	public void setMfr_desc(String mfrDesc) {
		mfr_desc = mfrDesc;
	}

	public String getHw_desc() {
		return hw_desc;
	}

	public void setHw_desc(String hwDesc) {
		hw_desc = hwDesc;
	}

	public String getSw_desc() {
		return sw_desc;
	}

	public void setSw_desc(String swDesc) {
		sw_desc = swDesc;
	}

	public String getSerial_num() {
		return serial_num;
	}

	public void setSerial_num(String serialNum) {
		serial_num = serialNum;
	}

	public String getDp_desc() {
		return dp_desc;
	}

	public void setDp_desc(String dpDesc) {
		dp_desc = dpDesc;
	}

	public HashMap<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(HashMap<String, String> attributes) {
		this.attributes = attributes;
	}

	

}
