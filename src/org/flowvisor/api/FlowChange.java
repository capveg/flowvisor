/**
 * 
 */
package org.flowvisor.api;


import org.flowvisor.flows.FlowEntry;

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
	FlowEntry entry;	
	public FlowChange() {
		// java beans constructor
	}

	public FlowChange(FlowChangeOp operation, int index, FlowEntry entry) {
		super();
		this.operation = operation;
		this.index = index;
		this.entry = entry;
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

	public FlowEntry getEntry() {
		return entry;
	}

	public void setEntry(FlowEntry entry) {
		this.entry = entry;
	}
	
}
