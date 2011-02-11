package org.flowvisor.flows;

import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.flowvisor.events.FVEventHandler;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVFlowMod;
import org.flowvisor.message.FVFlowRemoved;
import org.openflow.protocol.OFFlowMod;

/**
 * Internal DB for tracking the switch's state
 * 
 * NOT internally thread-safe
 * 
 * @author capveg
 * 
 */

public class LinearFlowDB implements FlowDB, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	SortedSet<FlowDBEntry> db;
	long dpid;
	transient FVEventHandler fvEventHandler;
	transient int flowID;

	public LinearFlowDB(FVEventHandler fvEventHandler) {
		this.db = new TreeSet<FlowDBEntry>();
		this.fvEventHandler = fvEventHandler;
		this.flowID = 0;
	}

	@Override
	public void processFlowMod(FVFlowMod flowMod, long dpid, String sliceName) {
		String op = "unknown";
		switch (flowMod.getCommand()) {
		case OFFlowMod.OFPFC_ADD:
			op = "ADD";
			processFlowModAdd(flowMod, sliceName, dpid);
			break;
		case OFFlowMod.OFPFC_MODIFY:
		case OFFlowMod.OFPFC_MODIFY_STRICT:
			op = "MOD";
			processFlowModModify(flowMod, sliceName, dpid);
			break;
		case OFFlowMod.OFPFC_DELETE:
		case OFFlowMod.OFPFC_DELETE_STRICT:
			op = "DEL";
			processFlowModDelete(flowMod, sliceName, dpid);
			break;
		default:
			FVLog.log(LogLevel.WARN, fvEventHandler,
					"flowDB: ignore fm with unknown flow_mod command:: ",
					flowMod.getCommand());
		}
		FVLog.log(LogLevel.DEBUG, null, "flowdb: ", op, ": new size ", size());
	}

	/**
	 * Remove one or more flowdb entries
	 * 
	 * fail silently if there is nothing deleted
	 * 
	 * @param flowMod
	 * @param sliceName
	 */

	private void processFlowModDelete(FVFlowMod flowMod, String sliceName,
			long dpid) {
	}

	/**
	 * Change one or more flowdb entries
	 * 
	 * fail silently if nothing matches
	 * 
	 * @param flowMod
	 * @param sliceName
	 */
	private void processFlowModModify(FVFlowMod flowMod, String sliceName,
			long dpid) {

	}

	/**
	 * Add a new flowdb entry
	 * 
	 * @param flowMod
	 * @param sliceName
	 * @param dpid
	 */
	private void processFlowModAdd(FVFlowMod flowMod, String sliceName,
			long dpid) {
		FlowDBEntry flowDBEntry = new FlowDBEntry(dpid, flowMod.getMatch(),
				this.flowID++, flowMod.getPriority(), flowMod.getActions(),
				sliceName, flowMod.getCookie());
		FVLog.log(LogLevel.DEBUG, this.fvEventHandler,
				"flowDB: adding new entry:", flowDBEntry);
		this.db.add(flowDBEntry);
	}

	@Override
	public void processFlowRemoved(FVFlowRemoved flowRemoved, long dpid) {
		boolean found = false;
		for (Iterator<FlowDBEntry> it = this.db.iterator(); it.hasNext();) {
			FlowDBEntry flowDBEntry = it.next();
			if (flowDBEntry.getRuleMatch().equals(flowRemoved.getMatch())
					&& flowDBEntry.getPriority() == flowRemoved.getPriority()
					&& flowDBEntry.getCookie() == flowRemoved.getCookie()
					&& flowDBEntry.getDpid() == dpid) {
				it.remove();
				found = true;
				FVLog.log(LogLevel.DEBUG, this.fvEventHandler,
						"flowDB: removing flow '", flowDBEntry,
						"'matching flowRemoved: ", flowRemoved);
				break;
			}
		}
		if (!found)
			FVLog.log(LogLevel.INFO, this.fvEventHandler,
					"flowDB: ignoring unmatched flowRemoved: ", flowRemoved);
	}

	@Override
	public Iterator<FlowDBEntry> iterator() {
		return this.db.iterator();
	}

	@Override
	public int size() {
		return this.db.size();
	}

}
