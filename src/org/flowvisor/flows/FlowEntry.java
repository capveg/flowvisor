/**
 * 
 */
package org.flowvisor.flows;

import java.util.List;
import org.openflow.protocol.*;
import org.openflow.protocol.action.*;

/**
 * @author capveg
 * Holds data "IF packets match this RULE, THEN perform list of ACTIONS on it"
 * In addition to normal openflow flow entry symantics, this flow entry also
 * matches on dpid
 */
public class FlowEntry implements FlowMatches {
	public static final long ALL_DPIDS = Long.MIN_VALUE;
	OFMatch ruleMatch;
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
		this.actionsList = 	actionsList;
	}
	
	public FlowEntry(OFMatch match, List<OFAction> actionsList) {
		this.dpid = ALL_DPIDS;
		this.ruleMatch = match;
		this.actionsList = actionsList;
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
	
	// private class to carry TestField results
	private class TestResult {
		int returnWildcard;
		Object result;
		boolean matchFound;
		boolean maybeSubset;
		boolean maybeSuperset;
		TestResult() {
			this.maybeSubset = this.maybeSuperset = this.matchFound = true;
		}
		void setTestResult(int wc, Object res, MatchType m) {
			this.returnWildcard |= wc; 
			this.result = res; 
			if (m == MatchType.NONE)
				matchFound = false;
			else {
				if(m == MatchType.SUPERSET)
					maybeSubset = false;
				if(m == MatchType.SUBSET)
					maybeSuperset = false;
			}
		}
	}
	
	/**
	 * Test whether the passed fields match and if so, how
	 * 
	 * @author capveg
	 *
	 * @param <A>
	 */
	private class TestField {
		/** 
		 * Given two Objects x,y from an OFMatch structure, and two wildcards saying whether these
		 * 	fields are wilcarded, return :
		 * 	either x if both are wildcarded (or y, doesn't matter)
		 * 	x, if only y is wildcarded
		 *  y, if only x is wildcarded
		 *  x if x == y (or y, doesn't matter)
		 *  NONE if x != y
		 *  
		 *  NOTE: both x and y must support a deep equals() operation 
		 *  
		 * @param result	Baggage to store the tripple return value
		 * @param wildIndex	OFMatch.OFPFW_* value to indicate which field we're testing
		 * @param wildX		The wildcards from the first match
		 * @param wildY		The wildcards from the second match
		 * @param x			The field to test from the first match
		 * @param y			The field to test from the second match
		 */
		
		Object testField(TestResult result, int wildIndex, int wildX, int wildY, Object x, Object y) {
			if (( (wildX & wildIndex) == 0 ) && 
					( (wildY & wildIndex) == 0 )) {		// is neither field wildcarded?
				if(x.equals(y)) 
					result.setTestResult(0, x, MatchType.EQUAL);
				else 
					result.setTestResult(0, null, MatchType.NONE); 
			} 
			else if ((wildX & wildIndex) != 0 )  	// is just X wildcarded? 
				result.setTestResult(0, y, MatchType.SUBSET);
			else if ((wildY & wildIndex) != 0 )		// is just Y wildcarded?
				result.setTestResult(0, x, MatchType.SUPERSET);
			else
				result.setTestResult(wildIndex, x, MatchType.EQUAL);
			return result.result;
		}
		short testFieldShort(TestResult result, int wildIndex, int wildX, int wildY, short x, short y) {
			return ((Short)testField(result, wildIndex, wildX, wildY, 
					Short.valueOf(x), 
					Short.valueOf(y))).shortValue();
		}
		long testFieldLong(TestResult result, int wildIndex, int wildX, int wildY, long x, long y) {
			return ((Long)testField(result, wildIndex, wildX, wildY, 
					Long.valueOf(x), 
					Long.valueOf(y))).longValue();
		}
		byte testFieldByte(TestResult result, int wildIndex, int wildX, int wildY, byte x, byte y) {
			return ((Byte)testField(result, wildIndex, wildX, wildY, 
					Byte.valueOf(x), 
					Byte.valueOf(y))).byteValue();
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
		FlowIntersect intersection = new FlowIntersect();
		int argWildcards = argMatch.getWildcards();
		int ruleWildcards = this.ruleMatch.getWildcards();
		
		/**
		 * NOTE: the logic here is protracted and convoluted.. and I can't think of a better
		 * way to do this... :-(
		 */
		
		TestField tester = new TestField();
		TestResult result = new TestResult();
		OFMatch interMatch = intersection.getMatch();
		
		// test DPID
		intersection.setDPID(
				tester.testFieldLong(result, 1, dpid == ALL_DPIDS ? 1 : 0 , this.dpid == ALL_DPIDS? 1: 0, 
						dpid, this.dpid)
		);
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);
		
		// test ether_dst
		interMatch.setDataLayerDestination( (byte[])
				tester.testField(result, OFMatch.OFPFW_DL_DST, argWildcards, ruleWildcards, 
						argMatch.getDataLayerDestination(),
						ruleMatch.getDataLayerDestination())
		);
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);
		
		// test ether_src
		interMatch.setDataLayerSource( (byte[])
				tester.testField(result, OFMatch.OFPFW_DL_SRC, argWildcards, ruleWildcards, 
						argMatch.getDataLayerSource(),
						ruleMatch.getDataLayerSource())
		);
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);
		
		// test ether_type
		interMatch.setDataLayerType( 
				tester.testFieldShort(result, OFMatch.OFPFW_DL_TYPE, argWildcards, ruleWildcards,
						argMatch.getDataLayerType(),
						ruleMatch.getDataLayerType())
		);
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);

		// test vlan_type
		interMatch.setDataLayerVirtualLan(
				tester.testFieldShort(result, OFMatch.OFPFW_DL_VLAN, argWildcards, ruleWildcards,
						argMatch.getDataLayerVirtualLan(),
						ruleMatch.getDataLayerVirtualLan())
				);
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);

		// test vlan_pcp
		interMatch.setDataLayerVirtualLanPriorityCodePoint(
				tester.testFieldByte(result, OFMatch.OFPFW_DL_VLAN_PCP, argWildcards, ruleWildcards, 
						argMatch.getDataLayerVirtualLanPriorityCodePoint(), 
						ruleMatch.getDataLayerVirtualLanPriorityCodePoint())
		);
		
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);
		
		
		/***
		 * DONE matching: if we got this far, there is at least
		 * an intersection 
		 */
		
		if (result.maybeSubset  && result.maybeSuperset) {
			intersection.setMatchType(MatchType.EQUAL);
		} else if( result.maybeSubset) {
			intersection.setMatchType(MatchType.SUBSET);
		} else if ( result.maybeSuperset) {
			intersection.setMatchType(MatchType.SUPERSET);
		} else
			intersection.setMatchType(MatchType.INTERSECT);
		
		interMatch.setWildcards(result.returnWildcard);
		intersection.setMatch(interMatch);
		return intersection;
		
	}
	
}
