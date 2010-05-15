package org.flowvisor.message;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowIntersect;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.action.OFAction;

public class FVFlowMod extends org.openflow.protocol.OFFlowMod 
		implements Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}
	
	/**
	 * FlowMod slicing
	 * 
	 * 1) make sure all actions are ok
	 * 
	 * 2) expand this FlowMod to the intersection of things in the given match
	 *  and the slice's flowspace
	 */

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVLog.log(LogLevel.DEBUG, fvSlicer, "recv from controller: " + this);
		FVMessageUtil.translateXid(this, fvClassifier, fvSlicer);
		
		// FIXME: sanity check buffer id
		
		// make sure the list of actions is kosher
		List<OFAction> actionsList = this.getActions();
		try {
			actionsList= FVMessageUtil.approveActions(actionsList, this.match, fvClassifier, fvSlicer);
		} catch (ActionDisallowedException e) {
			// FIXME : embed the error code in the ActionDisallowedException and pull it out here
			FVLog.log(LogLevel.WARN, fvSlicer, "EPERM bad actions: " + this);
			fvSlicer.getMsgStream().write(
					FVMessageUtil.makeErrorMsg(OFBadActionCode.OFPBAC_EPERM, this));
			return;
		}

		this.setActions(actionsList);
		
		// expand this match to everything that intersects the flowspace
		List<FlowIntersect> intersections = fvSlicer.getFlowSpace().intersects(
				fvClassifier.getDPID(), this.match); 
		
		for(FlowIntersect intersect : intersections) {
			try {
				FVFlowMod newFlowMod = (FVFlowMod) this.clone();
				newFlowMod.setMatch(intersect.getMatch());  // replace match with the intersection
				FVLog.log(LogLevel.DEBUG, fvClassifier, "send to switch: " + this);
				fvClassifier.getMsgStream().write(newFlowMod);
			} catch (CloneNotSupportedException e) {
				FVLog.log(LogLevel.CRIT, fvSlicer, "FlowMod does not implement clone()!?: " + e);
				return;
			}
		}
		
	}

}
