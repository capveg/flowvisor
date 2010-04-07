/**
 * 
 */
package org.flowvisor.flows;

import java.util.ArrayList;
import java.util.List;

import org.openflow.protocol.OFMatch;

/**
 * @author capveg
 *	Implements FlowMap, but in a slow and linear fashion.
 *
 * (Hopefully Peyman will implement something faster :-)
 * 
 */
public class LinearFlowMap implements FlowMap {

	
	List<FlowEntry> rules;
	
	public LinearFlowMap () {
		this.rules = new ArrayList<FlowEntry>();
	}
 	
	@Override
	public FlowEntry matches(long dpid, byte[] packetData) {
		OFMatch m = new OFMatch();
		m.loadFromPacket(packetData);
		List<FlowEntry> list = matches(dpid, m);
		if (list.size() > 1) 
			throw new RuntimeException("matching a single packet returned more than one FlowEntry::" +
					list.size());
		return list.get(0);
	}

	@Override
	public void addRule(int position, FlowEntry rule) {
		this.rules.add(position, rule);
	}

	@Override
	public int countRules() {
		return this.rules.size();
	}

	@Override
	public void removeRule(int position) {
		this.rules.remove(position);
		
	}

	/**
	 * Strip down the flow intersections to just the matching rules
	 */
	@Override
	public List<FlowEntry> matches(long dpid, OFMatch match) {
		List<FlowEntry> results = new ArrayList<FlowEntry>();
		List<FlowIntersect> interList = intersects(dpid, match);
		for(FlowIntersect inter: interList) {
			results.add(inter.getFlowEntry());
		}
		return results;
	}

	/**
	 * Step through each FlowEntry in order and match on it.
	 * If we get EQUALS or SUBSET, then stop.
	 * 
	 * IF we get SUPERSET or INTERSECT, then keep going and merge the results.
	 */

	@Override
	public List<FlowIntersect> intersects(long dpid, OFMatch match) {

		List<FlowIntersect> results = new ArrayList<FlowIntersect>();
		FlowIntersect intersect;
		MatchType matchType;
		boolean needMerge = false;

		for(FlowEntry rule : rules) {
			intersect = rule.matches(dpid, match);
			matchType = intersect.getMatchType();

			if(matchType == MatchType.NONE)
				continue;

			results.add(intersect);
			if ((matchType == MatchType.EQUAL) || (matchType == MatchType.SUBSET))
				break;
			if ((matchType == MatchType.INTERSECT) || (matchType == MatchType.SUPERSET))
				needMerge = true;
			else  // else, wtf?
				throw new RuntimeException("Unknown MatchType = " + intersect.getMatchType());			
		}
		if(needMerge && ( results.size() > 1 )) 
			return priorityMerge(results);	 // expensive, avoid if possible
		else 
			return results;
	}

	/**
	 * Step through all of the partially computed results, compute the intersections
	 * and remove the intersections by priority.
	 * 
	 * Could be O(n^2) in worst case, but we expect that intersetions are rare (?)
	 * @param mergeList List of all FlowEntry's from matches(), including overlaps.
	 * 
	 * Uses the fact that the order of the list is also the priority order
	 *  
	 *  FIXME :: come back and make this faster
	 *  
	 * @return A pruned list of just the non-completely-overlapping matches
	 */
	
	List<FlowIntersect> priorityMerge(List<FlowIntersect> mergeList) {
		List<FlowIntersect> results = new ArrayList<FlowIntersect>();
		boolean eclipsed;
		MatchType matchType;
		results.add(mergeList.get(0));
		mergeList.remove(0);
		
		for(FlowIntersect merge: mergeList) {
			eclipsed = false;
			for(FlowIntersect result : results) {
				/* is this new match eclipsed by previous entries?
				 *  
				 *  with each successive matches() call, the part that
				 *  over laps result is removed, so that if a merge rule
				 *  is not fully eclipsed by any one result, but is fully
				 *  eclipsed by a sum of results, we will catch that too
				 *  
				 *   FIXME: needs testing!
				 */ 
				merge = merge.getFlowEntry().matches(merge.getDpid(), 
							result.getMatch());
				matchType = merge.getMatchType();
				if ((matchType == MatchType.EQUAL) || (matchType == MatchType.SUBSET)) {
					eclipsed = true;
					break;
				}					
			}
			if (! eclipsed ) 			// add this match to the list iff it's 
				results.add(merge);  	// not complete eclipsed by something before it
		}
		return results;
	}

	@Override
	public List<FlowEntry> getRules() {
		return this.rules;
	}

}
