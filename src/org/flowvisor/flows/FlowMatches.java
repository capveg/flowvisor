package org.flowvisor.flows;

import org.openflow.protocol.OFMatch;

/** 
 * Interface for computing intersections between lists of flows.
 * 
 * @author capveg
 *
 */

public interface FlowMatches {
	public FlowIntersect matches(long dpid, OFMatch match);

}
