/**
 * 
 */
package org.flowvisor.api;


import java.util.HashMap;
import java.util.Map;

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
		CHANGE;
		
		static Map<Integer,FlowChangeOp> typeMap = new HashMap<Integer,FlowChangeOp>();
		static public FlowChangeOp ordToType(int i) {
			return FlowChangeOp.typeMap.get(Integer.valueOf(i));
		}
		FlowChangeOp() {
			FlowChangeOp.addMapping(Integer.valueOf(this.ordinal()), this);
		}
		private static void addMapping(Integer i, FlowChangeOp flowChangeOp) {
			FlowChangeOp.typeMap.put(i, flowChangeOp);
		}
	}
	int operation;
	int index;
	FlowEntry entry;	
	public FlowChange() {
		// java beans constructor
	}

	public FlowChange(FlowChangeOp operation, int index, FlowEntry entry) {
		super();
		this.operation = operation.ordinal();
		this.index = index;
		this.entry = entry;
	}

	public FlowChangeOp getOperation() {
		return FlowChangeOp.ordToType(operation);
	}

	public void setOperation(FlowChangeOp operation) {
		this.operation = operation.ordinal();
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
