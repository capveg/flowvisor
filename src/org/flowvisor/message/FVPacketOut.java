package org.flowvisor.message;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.lldp.LLDPUtil;
import org.flowvisor.slicer.FVSlicer;

import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFErrorType;
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
		
		// if it's LLDP, pass off to the LLDP hack
		if(LLDPUtil.handleLLDPFromController(this, fvClassifier, fvSlicer))
			return;
		
		OFMatch match = new OFMatch();
		match.loadFromPacket(this.getPacketData(), OFPort.OFPP_ALL.getValue());
		// TODO : for efficiency, do this lookup on the slice flowspace, not the switch
		List<FlowEntry> flowEntries = fvClassifier.getSwitchFlowMap().matches(
				fvClassifier.getSwitchInfo().getDatapathId(), 
				match);
		if ((flowEntries == null) ||(flowEntries.size() < 1)) {  	// didn't match anything
			FVLog.log(LogLevel.WARN, fvSlicer, "EPERM bad encap packet: " + this);
			this.sendEpermError(fvSlicer.getMsgStream());
			return;
		}
		List<OFAction> actionsList = this.getActions();
		try {
			actionsList= FVMessageUtil.approveActions(actionsList, match, fvClassifier, fvSlicer);
		} catch (ActionDisallowedException e) {
			FVLog.log(LogLevel.WARN, fvSlicer, "EPERM bad actions: " + this);
			this.sendActionsError(fvSlicer.getMsgStream());
			return;
		}

		this.setActions(actionsList);
		// really annoying; should be in the base class
		short count = FVMessageUtil.countActionsLen(actionsList);
		this.setActionsLength(count);
		this.setLength((short) (FVPacketOut.MINIMUM_LENGTH + count +this.getPacketData().length));
		// if we've gotten this far, everything is kosher
		fvClassifier.getMsgStream().write(this);
	}
		

	private void sendActionsError(OFMessageAsyncStream out) {
		OFError err = new FVError();
		err.setErrorType(OFErrorType.OFPET_BAD_REQUEST);
		err.setErrorCode(OFBadRequestCode.OFPBRC_EPERM);
		err.setOffendingMsg(this);
		out.write(err);
	}

	private void sendEpermError(OFMessageAsyncStream out) {
		OFError err = new FVError();
		err.setErrorType(OFErrorType.OFPET_BAD_ACTION);
		err.setErrorCode(OFBadRequestCode.OFPBRC_EPERM);
		err.setOffendingMsg(this);
		out.write(err);
	}
	
	// convenience function that Derickso doesn't want in main openflow.jar
	@Override
	public void setPacketData(byte[] packetData) {
		if (packetData == null)
			this.length = (short)(MINIMUM_LENGTH + actionsLength);
		else
			this.length = (short)(MINIMUM_LENGTH +
					actionsLength +
					packetData.length);
		this.packetData = packetData;
	}


}
