/**
 * 
 */
package org.flowvisor.flows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openflow.protocol.*;
import org.openflow.protocol.action.*;
import org.openflow.util.HexString;

/**
 * @author capveg
 * Holds data "IF packets match this RULE, THEN perform list of ACTIONS on it"
 * In addition to normal openflow flow entry symantics, this flow entry also
 * matches on dpid
 */
public class FlowEntry {

	public static final long ALL_DPIDS 		= Long.MIN_VALUE;
	public static final String ALL_DPIDS_STR 	= "all_dpids"; 

	OFMatch ruleMatch;
	int index;
	List<OFAction> actionsList;
	long dpid;
	/**
	 * IF switch is dpid and packet match's match, then perform action list
	 * @param dpid switch's datapath id (from FeaturesReply) or ALL_DPIDS
	 * @param match an openflow match structure
	 * @param actionsList list of actions; empty list implies DROP
	 */
	public FlowEntry(long dpid, OFMatch match, List<OFAction> actionsList) {
		this.dpid = 		dpid;
		this.ruleMatch = 		match;
		this.index = -1;
		this.actionsList = 	actionsList;
	}
	
	public FlowEntry(long dpid, OFMatch match, OFAction action) {
		this(dpid,match,(List<OFAction>)null);
		this.actionsList = new ArrayList<OFAction>();
		this.actionsList.add(action);
	}
	
	public FlowEntry(OFMatch match, List<OFAction> actionsList) {
		this(ALL_DPIDS,match,actionsList);
	}

	public FlowEntry(OFMatch match, OFAction action) {
		this(ALL_DPIDS, match, action);
	}
	
	public FlowEntry() {
		this.index = -1; // set nothing; java bean
	}
	
	public OFMatch getMatch() {
		return this.ruleMatch;
	}
	public void setMatch(OFMatch match) {
		this.ruleMatch = match;
	}
	
	public long getDPID() {
		return this.dpid;
	}
	public void setDPID(long dpid ) {
		this.dpid = dpid;
	}
	
	public List<OFAction> getActionsList() {
		return this.actionsList;
	}
	public void setActionsList(List<OFAction> actionsList) {
		this.actionsList = actionsList;
	}
	
	private enum TestOp {
		LEFT,				// no test, just return the LEFT param
		RIGHT,				// no test, just return the RIGHT param
		EQUAL,				// test if they are equal
		WILD				// no test, both are wildcards
	}
	
	/**
	 * Test whether the passed fields match and if so, how
     * 
     * Code is a bit hackish, but not sure how to improve
	 * 
	 * @author capveg
	 */
	private class TestField {
		/**
		 * What type of test should we do on these params?
		 * 
		 * This is the common code for all of the tests
		 * 
		 * @param flowIntersect
		 * @param wildIndex
		 * @param wildX
		 * @param wildY
		 * @return
		 */
		TestOp whichTest(FlowIntersect flowIntersect, int wildIndex, int wildX, int wildY) {
			TestOp ret ;
			if (( (wildX & wildIndex) == 0 ) && 
					( (wildY & wildIndex) == 0 )) {		// is neither field wildcarded?
				ret = TestOp.EQUAL; 
			} 
			else if ((wildX & wildIndex) != 0 ) {  	// is just X wildcarded? 
				ret = TestOp.RIGHT;
				flowIntersect.maybeSubset = false;
			}
			else if ((wildY & wildIndex) != 0 ) {		// is just Y wildcarded?
				ret = TestOp.LEFT;
				flowIntersect.maybeSuperset = false;
			}
			else
				ret = TestOp.WILD;
			
			OFMatch interMatch = flowIntersect.getMatch();
			int wildCards = interMatch.getWildcards();
			if (ret != TestOp.WILD) 
				interMatch.setWildcards(wildCards& (~wildIndex)); 	// disable wildCards for this field
			else
				interMatch.setWildcards(wildCards|wildIndex);		// enable wildCards for this field
			return ret;
		}
					
		short testFieldShort(FlowIntersect flowIntersect, int wildIndex, int wildX, int wildY, short x, short y) {
			
			switch(whichTest(flowIntersect, wildIndex, wildX, wildY)) {
			case EQUAL:
				if (x != y) 
					flowIntersect.setMatchType(MatchType.NONE);
				return x;		// or y; doesn't matter if they are equal
			case LEFT:
				return x;
			case WILD:			// or y; doesn't matter if they are wild
			case RIGHT:
				return y;
			}	
			assert(false);		// should never get here
			return -1;
		}
		
		long testFieldLong(FlowIntersect flowIntersect, int wildIndex, int wildX, int wildY, long x, long y) {
			
			switch(whichTest(flowIntersect, wildIndex, wildX, wildY)) {
			case EQUAL:
				if (x != y) 
					flowIntersect.setMatchType(MatchType.NONE);
				return x;		// or y; doesn't matter if they are equal
			case LEFT:
				return x;
			case WILD:			// or y; doesn't matter if they are wild
			case RIGHT:
				return y;
			}	
			assert(false);		// should never get here
			return -1;
		}
		byte testFieldByte(FlowIntersect flowIntersect, int wildIndex, int wildX, int wildY, byte x, byte y) {
			
			switch(whichTest(flowIntersect, wildIndex, wildX, wildY)) {
			case EQUAL:
				if (x != y) 
					flowIntersect.setMatchType(MatchType.NONE);
				return x;		// or y; doesn't matter if they are equal
			case LEFT:
				return x;
			case WILD:			// or y; doesn't matter if they are wild
			case RIGHT:
				return y;
			}	
			assert(false);		// should never get here
			return -1;
		}

		byte[] testFieldByteArray(FlowIntersect flowIntersect, int wildIndex, int wildX, int wildY, byte x[], byte y[]) {
			
			switch(whichTest(flowIntersect, wildIndex, wildX, wildY)) {
			case EQUAL:
				if (!Arrays.equals(x, y)) 
					flowIntersect.setMatchType(MatchType.NONE);
				return x;		// or y; doesn't matter if they are equal
			case LEFT:
				return x;
			case WILD:			// or y; doesn't matter if they are wild
			case RIGHT:
				return y;
			}	
			assert(false);		// should never get here
			return null;
		}

		
		
		// see if ip prefix x/masklenX intersects with y/masklenY (CIDR-style)
		int testFieldMask(FlowIntersect flowIntersect, int maskShift,
				int masklenX, int masklenY,
				int x, int y) {
			int min = Math.min(masklenX, masklenY);  // get the less specific address
			if (min >= 32) {  // silly work around to deal with lack of unsigned
				if (x != y) 
					flowIntersect.setMatchType(MatchType.NONE);
				return x;
			}
					 
			int mask = (1 << min) -1;	// min < 32, so no signed issues
			int min_encoded = 32-min;	// because OpenFlow does it backwards... grr
			if ((x & mask) != (y & mask))
				flowIntersect.setMatchType(MatchType.NONE);
			// else there is some overlap
			OFMatch interMatch = flowIntersect.getMatch();
			int wildCards = interMatch.getWildcards();
			// turn off all bits for this match and then turn on the used ones
			wildCards = (wildCards& ~((1 << OFMatch.OFPFW_NW_SRC_BITS) - 1) << maskShift) | 
						min_encoded << maskShift;
			if (masklenX < masklenY ) 
				flowIntersect.maybeSubset=false;
			else if (masklenX > masklenY )
				flowIntersect.maybeSuperset = false;
			// note that b/c of how CIDR addressing works, there is no overlap that is not a SUB or SUPERSET
			
			return (x & mask);
		}

	}
	
	/** 
	 * Describe the overlap between the passed (dpid, match) argument 
	 * with this rule and return the information in a FlowIntersect structure
	 * SUBSET implies that the arguments matches a subset of this rule
	 * SUPERSET that the arguments matches a superset of this rule
	 * 
	 * General algorithm: step through each possible element of match (dpid, src_ip, etc.)
	 * 
	 * for each element, check if they are both wildcarded
	 * 	if arg is a wild card but rule is not, then not a subset
	 *  if arg is not a wild card, but rule is, then not a super set
	 *  if neither are wildcard, then make sure the values match else return NONE
	 * 
	 * NOTE: if you want to match a packet against a rule, first convert the packet to 
	 * 	an OFMatch using match.loadFromPacket()
	 * 
	 * @param dpid switch's DPID or 
	 * @param match
	 * @return An FlowIntersect struture that describes the match
	 */
	public FlowIntersect matches(long dpid, OFMatch argMatch) {
		FlowIntersect intersection = new FlowIntersect(this);
		int argWildcards = argMatch.getWildcards();
		int ruleWildcards = this.ruleMatch.getWildcards();
		
		/**
		 * NOTE: the logic here is protracted and error prone...but I can't think of a better
		 * way to do this... :-(
		 */
		
		// FIXME: lots of untested code
		
		TestField tester = new TestField();
		OFMatch interMatch = intersection.getMatch();
		
		// test DPID
		intersection.setDPID(
				tester.testFieldLong(intersection, 1, dpid == ALL_DPIDS ? 1 : 0 , this.dpid == ALL_DPIDS? 1: 0, 
						dpid, this.dpid)
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		// test in_port
		interMatch.setInputPort(
				tester.testFieldShort(intersection, OFMatch.OFPFW_IN_PORT, argWildcards, ruleWildcards, 
						argMatch.getInputPort(),
						ruleMatch.getInputPort())
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		
		// test ether_dst
		interMatch.setDataLayerDestination( 
				tester.testFieldByteArray(intersection, OFMatch.OFPFW_DL_DST, argWildcards, ruleWildcards, 
						argMatch.getDataLayerDestination(),
						ruleMatch.getDataLayerDestination())
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		// test ether_src
		interMatch.setDataLayerSource(
				tester.testFieldByteArray(intersection, OFMatch.OFPFW_DL_SRC, argWildcards, ruleWildcards, 
						argMatch.getDataLayerSource(),
						ruleMatch.getDataLayerSource())
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		// test ether_type
		interMatch.setDataLayerType( 
				tester.testFieldShort(intersection, OFMatch.OFPFW_DL_TYPE, argWildcards, ruleWildcards,
						argMatch.getDataLayerType(),
						ruleMatch.getDataLayerType())
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		// test vlan_type
		interMatch.setDataLayerVirtualLan(
				tester.testFieldShort(intersection, OFMatch.OFPFW_DL_VLAN, argWildcards, ruleWildcards,
						argMatch.getDataLayerVirtualLan(),
						ruleMatch.getDataLayerVirtualLan())
				);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		// test vlan_pcp
		interMatch.setDataLayerVirtualLanPriorityCodePoint(
				tester.testFieldByte(intersection, OFMatch.OFPFW_DL_VLAN_PCP, argWildcards, ruleWildcards, 
						argMatch.getDataLayerVirtualLanPriorityCodePoint(), 
						ruleMatch.getDataLayerVirtualLanPriorityCodePoint())
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		// test ip_dst
		interMatch.setNetworkDestination(
				tester.testFieldMask(intersection, OFMatch.OFPFW_NW_DST_SHIFT, 
						argMatch.getNetworkDestinationMaskLen(),
						ruleMatch.getNetworkDestinationMaskLen(),
						argMatch.getNetworkDestination(), 
						ruleMatch.getNetworkDestination())
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		// test ip_src
		interMatch.setNetworkSource(
				tester.testFieldMask(intersection, OFMatch.OFPFW_NW_SRC_SHIFT, 
						argMatch.getNetworkSourceMaskLen(),
						ruleMatch.getNetworkSourceMaskLen(),
						argMatch.getNetworkSource(), 
						ruleMatch.getNetworkSource())
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		
		
		// test ip_proto
		interMatch.setNetworkProtocol(
				tester.testFieldByte(intersection, OFMatch.OFPFW_NW_PROTO, argWildcards, ruleWildcards, 
						argMatch.getNetworkProtocol(), 
						ruleMatch.getNetworkProtocol())
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		// test tp_src
		interMatch.setTransportSource(
				tester.testFieldShort(intersection, OFMatch.OFPFW_TP_SRC, argWildcards, ruleWildcards, 
						argMatch.getTransportSource(), 
						ruleMatch.getTransportSource())
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		// test tp_dst
		interMatch.setTransportDestination(
				tester.testFieldShort(intersection, OFMatch.OFPFW_TP_DST, argWildcards, ruleWildcards, 
						argMatch.getTransportDestination(), 
						ruleMatch.getTransportDestination())
		);
		if (intersection.getMatchType() == MatchType.NONE) 
			return intersection;	// shortcut back
		
		
		/***
		 * DONE matching: if we got this far, there is at least
		 * an intersection 
		 */
		
		if (intersection.maybeSubset  && intersection.maybeSuperset) {
			intersection.setMatchType(MatchType.EQUAL);
		} else if( intersection.maybeSubset) {
			intersection.setMatchType(MatchType.SUBSET);
		} else if ( intersection.maybeSuperset) {
			intersection.setMatchType(MatchType.SUPERSET);
		} else
			intersection.setMatchType(MatchType.INTERSECT);
		// wildcards was being set all of the way
		intersection.setMatch(interMatch);
		return intersection;
		
	}

	@Override
	public String toString() {
		String actions = "";
		for(OFAction action : actionsList) 
			actions += action.toString() +",";
		String dpid_str;
		if(this.dpid == ALL_DPIDS)
			dpid_str = ALL_DPIDS_STR;
		else
			dpid_str = HexString.toHexString(this.dpid);
		String indexStr;
		if (index != -1)
			indexStr = ",index=" + index;
		else 
			indexStr = "";
		
		return "FlowEntry[dpid=[" + dpid_str +
				"],ruleMatch=[" + this.ruleMatch + "],actionsList=[" + actions + "]" + indexStr+ "]";
	}

	/**
	 * Parse the output from this.toString() and return a matching
	 * FlowEntry
	 * 
	 * Minimal error checking
	 * 
	 * @param string
	 * @return an initialized flowentry
	 */
	public static FlowEntry fromString(String string) {
		String[] tokens = string.split("[]");
		List<OFAction> actionsList = new ArrayList<OFAction>();
		long dpid;
		OFMatch rule ;
		if (!tokens[0].equals("FlowEntry"))
			throw new IllegalArgumentException("expected FlowEntry, got '" + tokens[0]+ "'");
		if (!tokens[1].equals("dpid="))
			throw new IllegalArgumentException("expected dpid=, got '" + tokens[1]+ "'");
		int i;
		// translate dpid
		if (tokens[2].equals(ALL_DPIDS_STR))
			dpid = ALL_DPIDS;
		else
			dpid = HexString.toLong(tokens[2]);
		rule = new OFMatch();
		rule.fromString(tokens[4]);
		String [] actions = tokens[6].split(",");
		for (i=0; i < actions.length ; i++ )
			if(! actions[i].equals(""))
				actionsList.add(OFAction.fromString(actions[i]));
		
		return new FlowEntry(dpid, rule, actionsList);
	}

	public OFMatch getRuleMatch() {
		return ruleMatch;
	}

	public void setRuleMatch(OFMatch ruleMatch) {
		this.ruleMatch = ruleMatch;
	}

	public long getDpid() {
		return dpid;
	}

	public void setDpid(long dpid) {
		this.dpid = dpid;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}
