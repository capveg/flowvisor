package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;

import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.lldp.LLDPUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.*;
import org.openflow.protocol.action.OFAction;

public class FVPacketIn extends OFPacketIn implements Classifiable, Slicable {

	/**
	 * route and rewrite packet_in messages from switch to controller
	 * 
	 * if it's lldp, do the lldp decode stuff
	 * else, look up the embedded packet's controller(s) by flowspace and
	 * send to them
	 */
	
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// handle LLDP as a special (hackish) case
		if (LLDPUtil.handleLLDPFromSwitch(this, fvClassifier))
			return;
		// TODO add  ARP special case
		this.lookupByFlowSpace(fvClassifier);

	}

	private void lookupByFlowSpace(FVClassifier fvClassifier) {
		SliceAction sliceAction;
		int perms;
		// grab single matching rule: only one because it's a point in flowspace
		FlowEntry flowEntry = fvClassifier.getSwitchFlowMap().matches(
					fvClassifier.getSwitchInfo().getDatapathId(), 
					this.getInPort(),
					this.getPacketData()
					);
		if(flowEntry == null) {
			FVLog.log(LogLevel.WARN, fvClassifier, "dropping unclassifiable msg: " + this);
			return;
		}
		// foreach slice in that rule 
		for(OFAction ofAction : flowEntry.getActionsList()) {
			sliceAction = (SliceAction)  ofAction;
			perms = sliceAction.getSlicePerms();
			if ( (perms & (SliceAction.READ|SliceAction.WRITE)) != 0) {
				// lookup slice and send msg to them
				// TODO record buffer id for later validation
				FVSlicer fvSlicer = fvClassifier.getSlicerMap().get(sliceAction.getSliceName());
				FVLog.log(LogLevel.DEBUG, fvSlicer, "sending to controller: " + this);
				fvSlicer.getMsgStream().write(this);
			}
		}
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + this);
	}

	@Override
	public FVPacketIn setPacketData(byte[] packetData) {
		if (packetData == null)
			this.length = (short)(MINIMUM_LENGTH);
		else
			this.length = (short)(MINIMUM_LENGTH +
					packetData.length);
		this.packetData = packetData;
		return this;
	}

}
