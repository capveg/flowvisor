/**
 * 
 */
package org.flowvisor.api;

import java.util.HashMap;

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
	
}
