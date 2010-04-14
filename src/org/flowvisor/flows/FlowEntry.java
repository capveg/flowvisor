/**
 * 
 */
package org.flowvisor.flows;

import java.util.ArrayList;
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
	
	public FlowEntry(long dpid, OFMatch match, OFAction action) {
		this.dpid = dpid;
		this.ruleMatch = match;
		this.actionsList = new ArrayList<OFAction>();
		this.actionsList.add(action);
	}
	
	public FlowEntry(OFMatch match, List<OFAction> actionsList) {
		this.dpid = ALL_DPIDS;
		this.ruleMatch = match;
		this.actionsList = actionsList;
	}

	public FlowEntry(OFMatch match, OFAction action) {
		this.dpid = ALL_DPIDS;
		this.ruleMatch = match;
		this.actionsList = new ArrayList<OFAction>();
		this.actionsList.add(action);
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
			Short s = ((Short)testField(result, wildIndex, wildX, wildY, 
					Short.valueOf(x), 
					Short.valueOf(y)));
			if (s != null)
				return s.shortValue();
			else 
				return -1;
		}
		long testFieldLong(TestResult result, int wildIndex, int wildX, int wildY, long x, long y) {
			Long l =  ((Long)testField(result, wildIndex, wildX, wildY, 
					Long.valueOf(x), 
					Long.valueOf(y)));
			if (l != null)
				return l.longValue();
			else 
				return -1;
		}
		byte testFieldByte(TestResult result, int wildIndex, int wildX, int wildY, byte x, byte y) {
			Byte b = (Byte)testField(result, wildIndex, wildX, wildY, 
					Byte.valueOf(x), 
					Byte.valueOf(y));
			if (b != null) 
				return b.byteValue();
			else 
				return -1;
		}
		// see if ip prefix x/masklenX intersects with y/masklenY (CIDR-style)
		int testFieldMask(TestResult result, int maskShift,
				int masklenX, int masklenY,
				int x, int y) {
			int min = Math.min(masklenX, masklenY);  // get the less specific address
			if (min >= 32) {  // silly work around to deal with lack of unsigned
				if (x == y) 
					result.setTestResult(0, Integer.valueOf(x), MatchType.EQUAL);
				else
					result.setTestResult(0, Integer.valueOf(x), MatchType.NONE);
				return x;
			}
					 
			int mask = (1 << min) -1;	// min < 32, so no signed issues
			int min_encoded = 32-min;	// because OpenFlow does it backwards... grr
			if ((x & mask) != (y & mask))
				result.setTestResult(0, Integer.valueOf(x), MatchType.NONE);
			// else there is some overlap
			if (masklenX < masklenY ) 
				result.setTestResult(min_encoded << maskShift, Integer.valueOf(x & mask), MatchType.SUPERSET);
			else if (masklenX > masklenY )
				result.setTestResult(min_encoded << maskShift, Integer.valueOf(x & mask), MatchType.SUBSET);
			else
				result.setTestResult(min_encoded << maskShift, Integer.valueOf(x & mask), MatchType.EQUAL);
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
		TestResult result = new TestResult();
		OFMatch interMatch = intersection.getMatch();
		
		// test DPID
		intersection.setDPID(
				tester.testFieldLong(result, 1, dpid == ALL_DPIDS ? 1 : 0 , this.dpid == ALL_DPIDS? 1: 0, 
						dpid, this.dpid)
		);
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);
		
		// test in_port
		interMatch.setInputPort(
				tester.testFieldShort(result, OFMatch.OFPFW_IN_PORT, argWildcards, ruleWildcards, 
						argMatch.getInputPort(),
						ruleMatch.getInputPort())
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
		
		// test ip_dst
		interMatch.setNetworkDestination(
				tester.testFieldMask(result, OFMatch.OFPFW_NW_DST_SHIFT, 
						argMatch.getNetworkDestinationMaskLen(),
						ruleMatch.getNetworkDestinationMaskLen(),
						argMatch.getNetworkDestination(), 
						ruleMatch.getNetworkDestination())
		);
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);
		
		// test ip_src
		interMatch.setNetworkSource(
				tester.testFieldMask(result, OFMatch.OFPFW_NW_SRC_SHIFT, 
						argMatch.getNetworkSourceMaskLen(),
						ruleMatch.getNetworkSourceMaskLen(),
						argMatch.getNetworkSource(), 
						ruleMatch.getNetworkSource())
		);
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);
		
		
		
		// test ip_proto
		interMatch.setNetworkProtocol(
				tester.testFieldByte(result, OFMatch.OFPFW_NW_PROTO, argWildcards, ruleWildcards, 
						argMatch.getNetworkProtocol(), 
						ruleMatch.getNetworkProtocol())
		);
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);
		
		// test tp_src
		interMatch.setTransportSource(
				tester.testFieldShort(result, OFMatch.OFPFW_TP_SRC, argWildcards, ruleWildcards, 
						argMatch.getTransportSource(), 
						ruleMatch.getTransportSource())
		);
		if (! result.matchFound) 
			return intersection.setMatchType(MatchType.NONE);
		
		// test tp_dst
		interMatch.setTransportDestination(
				tester.testFieldShort(result, OFMatch.OFPFW_TP_DST, argWildcards, ruleWildcards, 
						argMatch.getTransportDestination(), 
						ruleMatch.getTransportDestination())
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
		
		return "FlowEntry[dpid=[" + dpid_str +
				"],ruleMatch=[" + this.ruleMatch + "],actionsList=[" + actions + "],]";
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
	
	
	
	

	
}
