/**
 * 
 */
package org.flowvisor.flows;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.flowvisor.events.FVEventHandler;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;

/**
 * @author capveg
 * 
 */
public class LinearFlowRewriteDB implements FlowRewriteDB {

	private static final long serialVersionUID = 1L;

	FVEventHandler fvEventHandler;
	Map<FlowDBEntry, FlowDB> map;
	Map<FlowDBEntry, FlowDBEntry> reverseMap;
	String sliceName;
	long dpid;

	public LinearFlowRewriteDB(FVEventHandler fvEventHandler, String sliceName,
			long dpid) {
		this.fvEventHandler = fvEventHandler;
		this.map = new HashMap<FlowDBEntry, FlowDB>();
		this.reverseMap = new HashMap<FlowDBEntry, FlowDBEntry>();
		this.sliceName = sliceName;
		this.dpid = dpid;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.flowvisor.flows.FlowRewriteDB#processFlowMods(org.flowvisor.message
	 * .FVFlowMod, org.flowvisor.message.FVFlowMod, long, java.lang.String)
	 */
	@Override
	public void processFlowMods(OFFlowMod original, OFFlowMod rewrite) {
		// FIXME: think about how to change API to prevent
		// original->originalEntry conversion with each call
		FlowDBEntry originalEntry = new FlowDBEntry(dpid, 0, original,
				sliceName);
		FlowDB flowDB;
		if (!map.containsKey(originalEntry)) {
			flowDB = new LinearFlowDB(fvEventHandler);
			map.put(originalEntry, flowDB);
		} else
			flowDB = map.get(originalEntry);
		// store forward map
		flowDB.processFlowMod(rewrite, dpid, sliceName);
		if (flowDB.size() == 0)
			this.map.remove(original);
		else {
			// store reverse map, but only on add
			FlowDBEntry rewriteEntry = new FlowDBEntry(dpid, 0, rewrite,
					sliceName);
			rewriteEntry.setActionsList(null); // so it matches with FlowRemoved
			reverseMap.put(rewriteEntry, originalEntry);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.flowvisor.flows.FlowRewriteDB#processFlowRemoved(org.flowvisor.message
	 * .FVFlowRemoved, long)
	 */
	@Override
	public void processFlowRemoved(OFFlowRemoved flowRemoved) {
		FlowDBEntry removedEntry = new FlowDBEntry(dpid, 0, flowRemoved,
				sliceName);
		if (!reverseMap.containsKey(removedEntry)) {
			FVLog.log(LogLevel.WARN, fvEventHandler,
					"flowrewriteDB: tried to remove non-existent flow ",
					flowRemoved);
			return;
		}
		FlowDBEntry original = reverseMap.get(removedEntry);
		reverseMap.remove(removedEntry);
		FlowDB flowDB = map.get(original);
		if (flowDB == null) {
			FVLog.log(
					LogLevel.WARN,
					fvEventHandler,
					"flowrewriteDB: internal corruption; flow exists in reverse but not forward map: ",
					flowRemoved);
			return;
		}
		flowDB.processFlowRemoved(flowRemoved, dpid);
		if (flowDB.size() == 0)
			this.map.remove(original);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.flows.FlowRewriteDB#originals()
	 */
	@Override
	public Set<FlowDBEntry> originals() {
		return this.map.keySet();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.flowvisor.flows.FlowRewriteDB#getRewrites(org.flowvisor.flows.FlowDBEntry
	 * )
	 */
	@Override
	public FlowDB getRewrites(FlowDBEntry original) {
		return this.map.get(original);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.flows.FlowRewriteDB#size()
	 */
	@Override
	public int size() {
		return this.map.size();
	}

}
