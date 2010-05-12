/**
 * 
 */
package org.flowvisor.api;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author capveg
 *
 */
public class LinkAdvertisement {
	String srcDPID;
	int srcPort;
	String dstDPID;
	int dstPort;
	// list of key=value pairs, for extensibility
	HashMap<String,String> attributes;
	
	protected LinkAdvertisement() {
		// do nothing, for a java bean
	}

	
	
	public LinkAdvertisement(String srcDPID, int srcPort, String dstDPID,
			int dstPort) {
		super();
		this.srcDPID = srcDPID;
		this.srcPort = srcPort;
		this.dstDPID = dstDPID;
		this.dstPort = dstPort;
	}



	public String getSrcDPID() {
		return srcDPID;
	}

	public void setSrcDPID(String srcDPID) {
		this.srcDPID = srcDPID;
	}

	public int getSrcPort() {
		return srcPort;
	}

	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}

	public String getDstDPID() {
		return dstDPID;
	}

	public void setDstDPID(String dstDPID) {
		this.dstDPID = dstDPID;
	}

	public int getDstPort() {
		return dstPort;
	}

	public void setDstPort(int dstPort) {
		this.dstPort = dstPort;
	}

	public HashMap<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(HashMap<String, String> attributes) {
		this.attributes = attributes;
	}
	
	public void setAttribute(String key, String value) {
		if(this.attributes == null)
			this.attributes = new HashMap<String,String>();
		this.attributes.put(key,value);
	}

	/**
	 * I *SWEAR* XMLRPC is supposed to be able to handle this for me... :-(
	 * 
	 * @return a key=value paired map of information on this link
	 */

	public Map<String, String> toMap() {
		Map<String,String> map = new HashMap<String,String>();
		map.put("srcDPID", srcDPID);
		map.put("srcPort",String.valueOf(srcPort));
		map.put("dstDPID", dstDPID);
		map.put("dstPort",String.valueOf(dstPort));
		String attribs = "";
		for(String attrib : attributes.keySet()) {
			if (!attribs.equals("")) 
				attribs+=",";
			attribs+=attrib + "=" + attributes.get(attrib);
		}
		map.put("attributes",attribs);
		return map;
	}
	
}
