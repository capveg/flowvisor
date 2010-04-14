/**
 * 
 */
package org.flowvisor.flows;

import org.flowvisor.config.*;
import org.openflow.protocol.*;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.HexString;

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
	 * @param dpid As returned in OFFeaturesReply
	 * @return A list of names of slices, i.e., "alice", "bob", etc.
	 */
	public static Set<String> getSlicesByDPID(long dpid) {
		Set<String> ret = new HashSet<String>();
		FlowMap flowmap = FVConfig.getFlowSpaceFlowMap();
		OFMatch match = new OFMatch();
		match.setWildcards(OFMatch.OFPFW_ALL);
		List<FlowEntry> rules = flowmap.matches(dpid, match);
		for(FlowEntry rule: rules) { 
			for(OFAction action : rule.getActionsList()) {
				SliceAction sliceAction = (SliceAction) action;	// the flowspace should only contain SliceActions
				ret.add(sliceAction.sliceName);
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
	 */
	
	public static void main(String args[] ) {
		if ((args.length != 1) && (args.length != 2)) {
			System.err.println("Usage: FLowSpaceUtil <dpid> [slice]");
			System.exit(1);
		}
		
		DefaultConfig.init();
		
		long dpid; 
		if(args[0].indexOf(':') != -1 )
			dpid = HexString.toLong(args[0]);
		else
			dpid = Long.valueOf(args[0]);
		
		
		switch(args.length) {
		case 1 :
			Set<String> slices = FlowSpaceUtil.getSlicesByDPID(dpid);
			System.out.println("The following slices have access to dpid=" + args[0]);
			for(String slice: slices)
				System.out.println(slice);
			break;
		case 2 :
			Set<Short> ports = FlowSpaceUtil.getPortsBySlice(dpid, args[1]);
			System.out.println("Slice " + args[1] + " on switch " + args[0] + 
					" has access to port:");
			if (ports.size() == 1 && ports.contains(Short.valueOf(OFPort.OFPP_ALL.getValue())) )
				System.out.println("ALL PORTS");
			else
				for(Short port: ports)
					System.out.println("Port: " + port);
		}
			
	}
}
