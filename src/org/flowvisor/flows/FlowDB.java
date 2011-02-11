/**
 * 
 */
package org.flowvisor.flows;

import java.util.Iterator;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;

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
	public void processFlowMod(OFFlowMod flowMod, long dpid, String sliceName);

	public void processFlowRemoved(OFFlowRemoved flowRemoved, long dpid);

	public Iterator<FlowDBEntry> iterator();

	public int size();
}
