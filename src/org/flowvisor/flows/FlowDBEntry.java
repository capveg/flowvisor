package org.flowvisor.flows;

import java.util.List;
import java.util.Map;

import org.flowvisor.config.BracketParse;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

public class FlowDBEntry extends FlowEntry {
	String sliceName;
	long cookie;
	private long creationTime;

	/**
	 * @return the creationTime
	 */
	public long getCreationTime() {
		return creationTime;
	}

	public FlowDBEntry(long dpid, OFMatch match, int flowID, int priority,
			List<OFAction> actionsList, String sliceName, long cookie) {
		super(dpid, match, flowID, priority, actionsList);
		this.sliceName = sliceName;
		this.cookie = cookie;
		this.creationTime = System.currentTimeMillis();
	}

	public FlowDBEntry() {
		// TODO Auto-generated constructor stub
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

	@Override
	public Map<String, String> toBracketMap() {
		Map<String, String> map = super.toBracketMap();
		super.toBracketMap();
		map.put("cookie", String.valueOf(cookie));
		map.put("slice", this.sliceName);
		map.put("duration",
				String.valueOf(System.currentTimeMillis() - this.creationTime));
		return map;
	}

	@Override
	public FlowDBEntry fromBacketMap(Map<String, String> map) {
		super.fromBacketMap(map);
		if (map.containsKey("cookie"))
			this.setCookie(Long.valueOf(map.get("cookie")));
		if (map.containsKey("slice"))
			this.setSliceName(map.get("slice"));
		if (map.containsKey("duration"))
			this.setCreationTime(System.currentTimeMillis()
					- Long.valueOf(map.get("duration")));
		return this;
	}

	private void setCreationTime(long l) {
		this.creationTime = l;
	}

	public static FlowEntry fromString(String string) {
		Map<String, String> map = BracketParse.decode(string);
		FlowDBEntry flowDBEntry = new FlowDBEntry();
		return flowDBEntry.fromBacketMap(map);
	}
}
