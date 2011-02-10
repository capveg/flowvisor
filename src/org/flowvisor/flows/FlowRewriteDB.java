package org.flowvisor.flows;

/**
 * Very similar to FlowDB, but instead of containing a list of 
 * flows in the switch, it contains the map of how flows were
 * rewritten by the flowvisor, i.e., 
 * 
 * alice sends flow f which the flowvisor rewrites as f1..fn
 * so FlowRewriteDB stores the "f --> f1..fn" mapping
 */

import java.io.Serializable;
import java.util.Set;

import org.flowvisor.message.FVFlowMod;
import org.flowvisor.message.FVFlowRemoved;

public interface FlowRewriteDB extends Serializable {
	/**
	 * Update the contents of the FlowDB with
	 * 
	 * @param flowMod
	 * @param Slicename
	 */

	public void processFlowMods(FVFlowMod original, FVFlowMod rewrite);

	public void processFlowRemoved(FVFlowRemoved flowRemoved);

	public Set<FlowDBEntry> originals();

	public FlowDB getRewrites(FlowDBEntry original);

	public int size();
}
