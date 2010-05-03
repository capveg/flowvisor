/**
 * 
 */
package org.flowvisor.api;

import java.util.List;

import org.flowvisor.flows.FlowEntry;
import org.openflow.protocol.action.OFAction;

/**
 * Object that holds a change to the flowspace
 * @author capveg
 *
 */
public class FlowChange {
	public enum FlowChangeOp {
		ADD,
		REMOVE,
		CHANGE
	}
	FlowChangeOp operation;
	int index;
	long dpid;
	FlowEntry entry;
	List<OFAction> actionsList;
	
	public FlowChange() {
		// java beans constructor
	}

	public FlowChange(FlowChangeOp operation, int index, long dpid,
			FlowEntry entry, List<OFAction> actionsList) {
		super();
		this.operation = operation;
		this.index = index;
		this.dpid = dpid;
		this.entry = entry;
		this.actionsList = actionsList;
	}

	public FlowChangeOp getOperation() {
		return operation;
	}

	public void setOperation(FlowChangeOp operation) {
		this.operation = operation;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public long getDpid() {
		return dpid;
	}

	public void setDpid(long dpid) {
		this.dpid = dpid;
	}

	public FlowEntry getEntry() {
		return entry;
	}

	public void setEntry(FlowEntry entry) {
		this.entry = entry;
	}

	public List<OFAction> getActionsList() {
		return actionsList;
	}

	public void setActionsList(List<OFAction> actionsList) {
		this.actionsList = actionsList;
	}
	
}
