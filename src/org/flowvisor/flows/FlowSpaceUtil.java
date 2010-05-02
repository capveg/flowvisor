/**
 * 
 */
package org.flowvisor.flows;

import org.flowvisor.config.*;
import org.openflow.protocol.*;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.HexString;

import java.io.FileNotFoundException;
import java.util.Set;
import java.util.List;
import java.util.HashSet;

/**
 * @author capveg
 *
 */
public class FlowSpaceUtil {
	/**
	 * Consult the FlowSpace and get a list of all slices that
	 * 	get connections to this switch, as specified by it's DPID
	 * 
	 * This function is somewhat expensive (think DB join), so the results
	 * should be cached, and then updated when the FlowSpace signals a change
	 * 
	 * @param flowMap A map of flow entries, like that from FVConfig.getFlowSpaceFlowMap();
	 * @param dpid As returned in OFFeaturesReply
	 * @return A list of names of slices, i.e., "alice", "bob", etc.
	 */
	public static Set<String> getSlicesByDPID(FlowMap flowMap, long dpid) {
		Set<String> ret = new HashSet<String>();
		OFMatch match = new OFMatch();
		match.setWildcards(OFMatch.OFPFW_ALL);
		List<FlowEntry> rules = flowMap.matches(dpid, match);
		for(FlowEntry rule: rules) { 
			for(OFAction action : rule.getActionsList()) {
				SliceAction sliceAction = (SliceAction) action;	// the flowspace should only contain SliceActions
				ret.add(sliceAction.sliceName);
			}
		}
		return ret;
	}
	
	
	public static FlowMap getSliceFlowSpace(String sliceName) {
		OFMatch match = new OFMatch();
		FlowMap ret = new LinearFlowMap();
		match.setWildcards(OFMatch.OFPFW_ALL);
		FlowMap flowSpace = FVConfig.getFlowSpaceFlowMap();
		List<FlowIntersect> intersections = flowSpace.intersects(FlowEntry.ALL_DPIDS, match);
		for(FlowIntersect inter: intersections) { 
			FlowEntry rule = inter.getFlowEntry();
			for(OFAction action : rule.getActionsList()) {
				SliceAction sliceAction = (SliceAction) action;	// the flowspace should only contain SliceActions
				if (sliceAction.getSliceName().equals(sliceName)) {
					FlowEntry neoRule = new FlowEntry(rule.getMatch(), sliceAction);
					ret.add();
				}
			}
		}
		return ret;
	}
	
	/**
	 * Consult the flowspace and return the set of ports that this slice
	 * is supposed to use on this switch
	 * 
	 * This function is somewhat expensive (think DB join), so the results
	 * should be cached, and then updated when the FlowSpace signals a change
	 * 
	 * OFPort.OFPP_ALL (0xfffc) is used to describe that all ports are supposed to be used.
	 * If all ports are valid, then OFPP_ALL will be the only port returned. 
	 * 
	 * @param dpid the switch identifier (from OFFeaturesReply)
	 * @param slice The slices name, e.g., "alice"
	 * @return Set of ports
	 */
	public static Set<Short> getPortsBySlice(long dpid, String slice) {
		Set<Short> ret = new HashSet<Short>();
		FlowMap flowmap = FVConfig.getFlowSpaceFlowMap();
		OFMatch match = new OFMatch();
		match.setWildcards(OFMatch.OFPFW_ALL);
		boolean allPorts = false;
		List<FlowEntry> rules = flowmap.matches(dpid, match);
		for(FlowEntry rule: rules) { 
			for(OFAction action : rule.getActionsList()) {
				SliceAction sliceAction = (SliceAction) action;	// the flowspace should only contain SliceActions
				if (sliceAction.sliceName.equals(slice)) {
					OFMatch ruleMatch = rule.getMatch();
					if ((ruleMatch.getWildcards()& OFMatch.OFPFW_IN_PORT) != 0)
						allPorts = true;
					else 
						ret.add(ruleMatch.getInputPort());
				}					
			}
		}
		if (allPorts) {  // if we got one "match all ports", just replace everything 
			ret.clear(); // with OFPP_ALL
			ret.add(OFPort.OFPP_ALL.getValue());
		}
		return ret;
	}
	
	/**
	 * Mini-frontend for querying FlowSpace
	 * @param args
	 * @throws FileNotFoundException 
	 */
	
	public static void main(String args[] ) throws FileNotFoundException {
		if ((args.length != 2) && (args.length != 3)) {
			System.err.println("Usage: FLowSpaceUtil config.xml <dpid> [slice]");
			System.exit(1);
		}
		
		FVConfig.readFromFile(args[0]);
		long dpid; 
		if(args[1].indexOf(':') != -1 )
			dpid = HexString.toLong(args[1]);
		else
			dpid = Long.valueOf(args[1]);
		
		
		switch(args.length) {
		case 2 :
			Set<String> slices = FlowSpaceUtil.getSlicesByDPID(FVConfig.getFlowSpaceFlowMap(),dpid);
			System.out.println("The following slices have access to dpid=" + args[1]);
			for(String slice: slices)
				System.out.println(slice);
			break;
		case 3 :
			Set<Short> ports = FlowSpaceUtil.getPortsBySlice(dpid, args[2]);
			System.out.println("Slice " + args[2] + " on switch " + args[1] + 
					" has access to port:");
			if (ports.size() == 1 && ports.contains(Short.valueOf(OFPort.OFPP_ALL.getValue())) )
				System.out.println("ALL PORTS");
			else
				for(Short port: ports)
					System.out.println("Port: " + port);
		}
			
	}

	/**
	 * Get the FlowMap that is the intersection of the Master FlowSpace and
	 * this dpid
	 * @param dpid As returned from OFFeatureReply
	 * @return A valid flowmap (never null)
	 */

	public static FlowMap getSubFlowMap(long dpid) {
		// assumes that new OFMatch() matches everything
		return FlowSpaceUtil.getSubFlowMap(FVConfig.getFlowSpaceFlowMap(), dpid, new OFMatch());
	}


	/**
	 * Get the FlowMap that is the intersection of this FlowMap and the given flowSpace
	 * that is, any rule in the source flowmap that matches any part of dpid and match
	 * is added to the returned flowmap
	 * @param flowMap Source flow map
	 * @param dpid datapathId from OFFeaturesReply
	 * @param match a valid OFMatch() struture
	 * @return a valid flowMap (never null)
	 */

	public static FlowMap getSubFlowMap(FlowMap flowMap, long dpid, OFMatch match) {
		// TODO
		throw new RuntimeException("buggy!  need to fix!");
		/*
		FlowMap neoFlowMap = new LinearFlowMap();
		List<FlowEntry> rules = flowMap.matches(dpid, match);
		for (FlowEntry rule : rules)
			flowMap.addRule(flowMap.countRules(), rule);
		return neoFlowMap; 
		*/
	}


}
