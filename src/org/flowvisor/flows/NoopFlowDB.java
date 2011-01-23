/**
 * 
 */
package org.flowvisor.flows;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.message.FVFlowMod;
import org.flowvisor.message.FVFlowRemoved;

/**
 * implement the FlowDB interface, but do nothing used when FlowTracking is
 * disabled because it's easier and cleaner than having a lot of if/then's in
 * the code
 * 
 * @author capveg
 * 
 */
public class NoopFlowDB implements FlowDB {

	List<FlowDBEntry> emptyList;

	public NoopFlowDB() {
		this.emptyList = new LinkedList<FlowDBEntry>();
	}

	@Override
	public void processFlowMod(FVFlowMod flowMod, long dpid, String sliceName) {
		// do nothing
	}

	@Override
	public void processFlowRemoved(FVFlowRemoved flowRemoved, long dpid) {
		// do nothing
	}

	@Override
	public Iterator<FlowDBEntry> iterator() {
		return emptyList.iterator();
	}

	@Override
	public int size() {
		return 0;
	}

}
