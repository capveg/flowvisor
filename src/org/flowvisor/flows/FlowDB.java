/**
 * 
 */
package org.flowvisor.flows;

import java.util.Iterator;

import org.flowvisor.message.FVFlowMod;
import org.flowvisor.message.FVFlowRemoved;

/**
 * An FV-local copy of the flow table state on the switch
 * 
 * track flow_mods and flow_expire messages
 * 
 * @author capveg
 * 
 */
public interface FlowDB extends Iterable<FlowDBEntry> {
	/**
	 * Update the contents of the FlowDB with
	 * 
	 * @param flowMod
	 * @param Slicename
	 */
	public void processFlowMod(FVFlowMod flowMod, long dpid, String sliceName);

	public void processFlowRemoved(FVFlowRemoved flowRemoved, long dpid);

	public Iterator<FlowDBEntry> iterator();

	public int size();
}
