/**
 *
 */
package org.flowvisor.api;

import java.util.HashMap;
import java.util.Map;

import org.flowvisor.exceptions.MapUnparsable;
import org.flowvisor.flows.FlowSpaceUtil;

/**
 * 
 * @author capveg
 * 
 */
public class LinkAdvertisement {
	String srcDPID;
	short srcPort;
	String dstDPID;
	short dstPort;
	// list of key=value pairs, for extensibility
	HashMap<String, String> attributes;

	protected LinkAdvertisement() {
		// do nothing, for a java bean
	}

	public LinkAdvertisement(String srcDPID, short srcPort, String dstDPID,
			short dstPort) {
		super();
		this.srcDPID = srcDPID;
		this.srcPort = srcPort;
		this.dstDPID = dstDPID;
		this.dstPort = dstPort;
		this.attributes = new HashMap<String, String>();
	}

	public LinkAdvertisement(long srcDPID, short srcPort, long dstDPID,
			short dstPort) {
		this(FlowSpaceUtil.dpidToString(srcDPID), srcPort, FlowSpaceUtil
				.dpidToString(dstDPID), dstPort);
	}

	public String getSrcDPID() {
		return srcDPID;
	}

	public void setSrcDPID(String srcDPID) {
		this.srcDPID = srcDPID;
	}

	public short getSrcPort() {
		return srcPort;
	}

	public void setSrcPort(short srcPort) {
		this.srcPort = srcPort;
	}

	public String getDstDPID() {
		return dstDPID;
	}

	public void setDstDPID(String dstDPID) {
		this.dstDPID = dstDPID;
	}

	public short getDstPort() {
		return dstPort;
	}

	public void setDstPort(short dstPort) {
		this.dstPort = dstPort;
	}

	public HashMap<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(HashMap<String, String> attributes) {
		this.attributes = attributes;
	}

	public void setAttribute(String key, String value) {
		if (this.attributes == null)
			this.attributes = new HashMap<String, String>();
		this.attributes.put(key, value);
	}

	/**
	 * I *SWEAR* XMLRPC is supposed to be able to handle this for me... :-(
	 * 
	 * @return a key=value paired map of information on this link
	 */

	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("srcDPID", srcDPID);
		map.put("srcPort", String.valueOf(srcPort));
		map.put("dstDPID", dstDPID);
		map.put("dstPort", String.valueOf(dstPort));
		String attribs = "";
		for (String attrib : attributes.keySet()) {
			if (!attribs.equals(""))
				attribs += ",";
			attribs += attrib + "=" + attributes.get(attrib);
		}
		map.put("attributes", attribs);
		return map;
	}

	static public boolean checkKey(String keyname, Map<String, String> map)
			throws MapUnparsable {
		if (!map.containsKey(keyname))
			throw new MapUnparsable("key not found: " + keyname);
		return true;
	}

	static public LinkAdvertisement fromMap(Map<String, String> map)
			throws MapUnparsable {
		LinkAdvertisement ad;

		checkKey("srcDPID", map);
		checkKey("dstDPID", map);
		checkKey("srcPort", map);
		checkKey("dstPort", map);

		ad = new LinkAdvertisement(map.get("srcDPID"), Short.valueOf(map
				.get("srcPort")), map.get("dstDPID"), Short.valueOf(map
				.get("dstPort")));
		if (map.containsKey("attributes")) {
			String[] attribs = map.get("attributes").split(",");
			for (int i = 0; i < attribs.length; i++) {
				String[] keyvalue = attribs[i].split("=");
				ad.setAttribute(keyvalue[0], keyvalue[1]);
			}
		}

		return ad;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Link[srcDPID=" + srcDPID + ", srcPort=" + srcPort + ",dstDPID="
				+ dstDPID + ",dstPort=" + dstPort + ",attributes=" + attributes
				+ "]";
	}

}
