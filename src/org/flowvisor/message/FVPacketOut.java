package org.flowvisor.message;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.action.OFAction;

/**
 * Verify that this packet_out operation is allowed by slice definition,
 * in terms of destination port, the flowspace of the embedded packet, the buffer_id,
 * and the actions.
 * 
 * Send an error msg back to controller if it's not
 * 
 * @author capveg
 *
 */

public class FVPacketOut extends OFPacketOut implements Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: " + this);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO verify the buffer_id is one we're allowed to use from a packet_in that went to us
		
		// TODO : for efficiency, do this lookup on the slice flowspace, not the switch
		FlowEntry flowEntry = fvClassifier.getSwitchFlowMap().matches(
				fvClassifier.getSwitchInfo().getDatapathId(), 
				this.getInPort(), 
				this.getPacketData());
		if (flowEntry == null) {  	// didn't match anything
			this.sendEpermError();
			return;
		}
		List<OFAction> actionsList = this.getActions();
		OFMatch match = new OFMatch();
		match.loadFromPacket(this.getPacketData(), this.getInPort());
		try {
			this.setActions(FVMessageUtil.approveActions(actionsList, match, fvClassifier, fvSlicer));
		} catch (ActionDisallowedException e) {
			this.sendActionsError();
			return;
		}
		// if we've gotten this far, everything is kosher
		fvClassifier.getMsgStream().write(this);
	}
		

	private void sendActionsError() {
		// TODO Auto-generated method stub
		assert(false);
	}

	private void sendEpermError() {
		// TODO Auto-generated method stub
		assert(false);
	}

}
