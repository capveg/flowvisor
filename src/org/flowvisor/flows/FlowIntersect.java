/**
 * 
 */
package org.flowvisor.flows;

import org.openflow.protocol.*;

/**
 * Describe the intersection between two FlowEntry's.
 * 
 * Contains an MatchType and if MatchType != NONE, then a dpid and OFMatch structure that
 * describes the overlap
 * 
 * @author capveg
 *
 */
public class FlowIntersect {
	MatchType matchType;
	long dpid;
	OFMatch match;
	
	public FlowIntersect() {
		this.dpid = FlowEntry.ALL_DPIDS;
		this.match = new OFMatch();
		this.matchType = MatchType.NONE;
	}
	
	public long getDPID() {
		return this.dpid;
	}
	public void setDPID(long dpid) {
		this.dpid = dpid;
	}
	
	public MatchType getMatchType() {
		return this.matchType;
	}
	public FlowIntersect setMatchType( MatchType matchType) {
		this.matchType = matchType;
		return this;
	}
	
	public OFMatch getMatch() {
		return this.match;	
	}
	public void setMatch(OFMatch match) {
		this.match = match;
	}
}
