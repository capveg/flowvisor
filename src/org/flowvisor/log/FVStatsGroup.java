package org.flowvisor.log;

import java.util.HashMap;
import java.util.Map;

import org.flowvisor.classifier.FVSendMsg;
import org.openflow.protocol.OFType;

/**
 * A collection of FVStats, organized by their senders
 * 
 * @author capveg
 * 
 */

public class FVStatsGroup {
	Map<FVSendMsg, FVStats> group;
	FVStats total;

	public FVStatsGroup() {
		this.group = new HashMap<FVSendMsg, FVStats>();
		this.total = new FVStats();
	}

	public void increment(FVSendMsg from, OFType ofType) {
		FVStats stats = group.get(from);
		if (stats == null) {
			stats = new FVStats();
			group.put(from, stats);
		}
		stats.incrementCounter(ofType);
		total.incrementCounter(ofType);
	}

	public long get(FVSendMsg from, OFType ofType) {
		if (!group.containsKey(from))
			return 0;
		else
			return group.get(from).getCounter(ofType);
	}

	public FVStats getTotal() {
		return this.total;
	}

	public long getTotal(OFType ofType) {
		return this.total.getCounter(ofType);
	}

	public synchronized void zeroCounters() {
		group.clear();
		total.zeroCounters();
	}

	@Override
	public synchronized String toString() {
		StringBuffer ret = new StringBuffer();
		for (FVSendMsg fvSendMsg : group.keySet()) {
			if (ret.length() > 0)
				ret.append("\n");
			ret.append(fvSendMsg.toString());
			ret.append(" :: ");
			ret.append(group.get(fvSendMsg).toString());
		}
		ret.append("Total :: ");
		ret.append(total.toString());

		return ret.toString();
	}
}
