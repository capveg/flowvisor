package org.flowvisor.flows;

import java.util.List;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

public class FlowDBEntry extends FlowEntry {
	String sliceName;
	long cookie;
	private final long creationTime;

	/**
	 * @return the creationTime
	 */
	public long getCreationTime() {
		return creationTime;
	}

	public FlowDBEntry(long dpid, OFMatch match, int priority,
			List<OFAction> actionsList, String sliceName, long cookie) {
		super(dpid, match, 0, priority, actionsList);
		this.sliceName = sliceName;
		this.cookie = cookie;
		this.creationTime = System.currentTimeMillis();
	}

	/**
	 * @return the sliceName
	 */
	public String getSliceName() {
		return sliceName;
	}

	/**
	 * @param sliceName
	 *            the sliceName to set
	 */
	public void setSliceName(String sliceName) {
		this.sliceName = sliceName;
	}

	/**
	 * @return the cookie
	 */
	public long getCookie() {
		return cookie;
	}

	/**
	 * @param cookie
	 *            the cookie to set
	 */
	public void setCookie(long cookie) {
		this.cookie = cookie;
	}
}
