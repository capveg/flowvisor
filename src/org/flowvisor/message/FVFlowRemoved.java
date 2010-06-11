package org.flowvisor.message;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.action.OFAction;

public class FVFlowRemoved extends OFFlowRemoved implements Classifiable,
		Slicable {

	/**
	 * Current algorithm: send this flow expiration to *anyone* who could have
	 * inserted this flow
	 * 
	 * FIXME: this is dumb: we should record the state of who actually sent it
	 * and only send it back to that person.
	 */
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FlowMap flowSpace = fvClassifier.getSwitchFlowMap();
		List<FlowEntry> flowEntries = flowSpace.matches(fvClassifier.getDPID(),
				this.match);
		Set<String> slicesToUpdate = new HashSet<String>();
		// make a list of everyone who could have inserted this flow entry
		for (FlowEntry flowEntry : flowEntries) {
			for (OFAction ofAction : flowEntry.getActionsList()) {
				if (ofAction instanceof SliceAction) {
					SliceAction sliceAction = (SliceAction) ofAction;
					if ((sliceAction.getSlicePerms() & SliceAction.WRITE) != 0) {
						slicesToUpdate.add(sliceAction.getSliceName());
					}
				}
			}
		}
		// forward this msg to each of them
		for (String slice : slicesToUpdate) {
			FVSlicer fvSlicer = fvClassifier.getSlicerMap().get(slice);
			if (fvSlicer == null) {
				FVLog.log(LogLevel.CRIT, fvClassifier,
						"inconsistent state: missing fvSliver entry for: "
								+ slice);
				continue;
			}
			fvSlicer.sendMsg(this); // actually send it to this slice
		}
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

}
