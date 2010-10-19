package org.flowvisor.message;

import java.io.FileNotFoundException;
import java.util.List;

import org.flowvisor.api.LinkAdvertisement;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.FVConfig;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.lldp.LLDPUtil;
import org.flowvisor.ofswitch.DPIDandPort;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.action.OFAction;

public class FVPacketIn extends OFPacketIn implements Classifiable, Slicable,
		TopologyControllable {

	/**
	 * route and rewrite packet_in messages from switch to controller
	 * 
	 * if it's lldp, do the lldp decode stuff else, look up the embedded
	 * packet's controller(s) by flowspace and send to them
	 */

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// handle LLDP as a special (hackish) case
		if (LLDPUtil.handleLLDPFromSwitch(this, fvClassifier))
			return;
		// TODO add ARP special case
		this.lookupByFlowSpace(fvClassifier);

	}

	private void lookupByFlowSpace(FVClassifier fvClassifier) {
		SliceAction sliceAction;
		int perms;
		// grab single matching rule: only one because it's a point in flowspace
		FlowEntry flowEntry = fvClassifier.getSwitchFlowMap().matches(
				fvClassifier.getSwitchInfo().getDatapathId(), this.getInPort(),
				this.getPacketData());
		if (flowEntry == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable msg: " + this.toVerboseString());
			return;
		}
		// foreach slice in that rule
		for (OFAction ofAction : flowEntry.getActionsList()) {
			sliceAction = (SliceAction) ofAction;
			perms = sliceAction.getSlicePerms();
			if ((perms & (SliceAction.READ | SliceAction.WRITE)) != 0) {
				// lookup slice and send msg to them
				// TODO record buffer id for later validation
				FVSlicer fvSlicer = fvClassifier.getSlicerByName(sliceAction
						.getSliceName());
				if (fvSlicer == null) {
					FVLog.log(LogLevel.WARN, fvClassifier,
							"tried to send msg to non-existant slice: "
									+ sliceAction.getSliceName()
									+ " corrupted flowspace?:: "
									+ this.toVerboseString());
					continue;
				}
				fvSlicer.sendMsg(this);
			}
		}
	}

	private String toVerboseString() {
		String pkt;
		if (this.packetData != null)
			pkt = new OFMatch().loadFromPacket(this.packetData, this.inPort)
					.toString();
		else
			pkt = "empty";
		return this.toString() + ";pkt=" + pkt;
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	@Override
	public FVPacketIn setPacketData(byte[] packetData) {
		if (packetData == null)
			this.length = (short) (MINIMUM_LENGTH);
		else
			this.length = (short) (MINIMUM_LENGTH + packetData.length);
		this.packetData = packetData;
		return this;
	}

	/**
	 * The topologyController handles LLDP messages and ignores everything else
	 */
	@Override
	public void topologyController(TopologyConnection topologyConnection) {
		synchronized (topologyConnection) {
			DPIDandPort dpidandport = TopologyConnection.parseLLDP(this
					.getPacketData());
			if (dpidandport == null) {
				FVLog
						.log(LogLevel.DEBUG, topologyConnection,
								"ignoring non-lldp packetin: "
										+ this.toVerboseString());
				return;
			}
			OFFeaturesReply featuresReply = topologyConnection
					.getFeaturesReply();
			if (featuresReply == null) {
				FVLog.log(LogLevel.WARN, topologyConnection,
						"ignoring packet_in: no features_reply yet");
				return;
			}
			LinkAdvertisement linkAdvertisement = new LinkAdvertisement(
					dpidandport.getDpid(), dpidandport.getPort(), featuresReply
							.getDatapathId(), this.inPort);
			topologyConnection.getTopologyController().reportProbe(
					linkAdvertisement);
			topologyConnection.signalFastPort(this.inPort);
		}
	}

	public static void main(String args[]) throws FileNotFoundException {
		if (args.length < 3) {
			System.err.println("Usage: <config.xml> <dpid> <ofmatch>");
			System.exit(1);
		}

		FVConfig.readFromFile(args[0]);
		long dpid = FlowSpaceUtil.parseDPID(args[1]);
		OFMatch packet = new OFMatch();
		packet.fromString(args[2]);

		System.err.println("Looking up packet '" + packet + "' on dpid="
				+ FlowSpaceUtil.dpidToString(dpid));
		List<FlowEntry> entries = FVConfig.getFlowSpaceFlowMap().matches(dpid,
				packet);

		System.err.println("Matches found: " + entries.size());
		if (entries.size() > 1)
			System.err.println("WARN: only sending to the first match");
		for (FlowEntry flowEntry : entries) {
			System.out.println(flowEntry);
		}

	}
}
