/**
 * 
 */
package org.flowvisor.ofswitch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.api.LinkAdvertisement;
import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.events.FVTimerEvent;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVFeaturesReply;
import org.flowvisor.message.FVMessageFactory;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.TopologyControllable;
import org.flowvisor.message.lldp.LLDPUtil;
import org.flowvisor.message.statistics.FVDescriptionStatistics;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;

/**
 * @author capveg
 * 
 */
public class TopologyConnection implements FVEventHandler {

	TopologyController topologyController;
	FVEventLoop pollLoop;
	SocketChannel sock;
	String name;
	OFMessageAsyncStream msgStream;
	FVMessageFactory fvMessageFactory;
	FVFeaturesReply featuresReply;
	FVDescriptionStatistics descriptionStatistics;

	public TopologyConnection(TopologyController topologyController,
			FVEventLoop pollLoop, SocketChannel sock) {
		this.topologyController = topologyController;
		this.pollLoop = pollLoop;
		this.sock = sock;
		this.name = "topo." + sock.toString();
		this.featuresReply = null;
		this.descriptionStatistics = null;
		this.fvMessageFactory = new FVMessageFactory();
		try {
			this.msgStream = new OFMessageAsyncStream(sock,
					this.fvMessageFactory);
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, this, "IOException in constructor!");
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#getThreadContext()
	 */
	@Override
	public long getThreadContext() {
		return Thread.currentThread().getId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.flowvisor.events.FVEventHandler#handleEvent(org.flowvisor.events.
	 * FVEvent)
	 */
	@Override
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		if (e instanceof FVIOEvent)
			handleIOEvent((FVIOEvent) e);
		else if (e instanceof FVTimerEvent)
			handleTimerEvent((FVTimerEvent) e);
		else
			throw new UnhandledEvent(e);
	}

	private void handleTimerEvent(FVTimerEvent e) {
		// TODO Auto-generated method stub

	}

	private void handleIOEvent(FVIOEvent e) {
		int ops = e.getSelectionKey().readyOps();

		try {
			// read stuff, if need be
			if ((ops & SelectionKey.OP_READ) != 0) {
				List<OFMessage> newMsgs = msgStream.read();
				if (newMsgs != null) {
					for (OFMessage m : newMsgs) {
						FVLog.log(LogLevel.DEBUG, this, "read " + m);
						if (m instanceof TopologyControllable)
							((TopologyControllable) m).topologyController(this);
						else
							FVLog.log(LogLevel.WARN, this,
									"ignoring unhandled msg: " + m);
					}
				} else {
					throw new IOException("got EOF from other side");
				}
			}
			// write stuff if need be
			if ((ops & SelectionKey.OP_WRITE) != 0)
				msgStream.flush();
		} catch (IOException e1) {
			// connection to switch died; tear it down
			FVLog.log(LogLevel.INFO, this,
					"got IO exception; closing because : " + e1);
			this.tearDown();
			return;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#needsAccept()
	 */
	@Override
	public boolean needsAccept() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#needsConnect()
	 */
	@Override
	public boolean needsConnect() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#needsRead()
	 */
	@Override
	public boolean needsRead() {
		return true; // always interested if there is something to read
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#needsWrite()
	 */
	@Override
	public boolean needsWrite() {
		if (this.msgStream == null)
			return false;
		else
			return msgStream.needsFlush();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#tearDown()
	 */
	@Override
	public void tearDown() {
		try {
			sock.close();
		} catch (IOException e) {
			FVLog.log(LogLevel.ALERT, this, "ignoring error on shutdown: " + e);
		}
	}

	/**
	 * Setup the connection
	 * 
	 * Queue up OFHello(), OFFeatureRequest(), and Stats Desc request messages
	 * 
	 * @throws IOException
	 */
	public void init() throws IOException {
		msgStream.write(this.fvMessageFactory.getMessage(OFType.HELLO));
		msgStream.write(this.fvMessageFactory
				.getMessage(OFType.FEATURES_REQUEST));

		// build stats desc request : FIXME: make this cleaner
		OFStatisticsRequest request = (OFStatisticsRequest) this.fvMessageFactory
				.getMessage(OFType.STATS_REQUEST);
		request.setStatisticType(OFStatisticsType.DESC);
		/*
		 * List<OFStatistics> statistics = new LinkedList<OFStatistics>();
		 * statistics.add(this.fvMessageFactory.getStatistics(
		 * OFType.STATS_REQUEST, OFStatisticsType.DESC));
		 * request.setStatistics(statistics);
		 */
		msgStream.write(request);

		msgStream.flush();
		int ops = SelectionKey.OP_READ;
		if (msgStream.needsFlush())
			ops |= SelectionKey.OP_WRITE;
		this.pollLoop.register(sock, ops, this);
	}

	/**
	 * Return whether this topology instance is fully connected and initialized.
	 * 
	 * Specifically, do we have responses to the initial features request and
	 * stats.desc requests
	 * 
	 * @return
	 */

	public boolean isConnected() {
		return this.featuresReply != null && this.descriptionStatistics != null;
	}

	/**
	 * Return the DPID of the switch associated with this TopologyConnection
	 * instance
	 * 
	 * @return null if featuresReply not yet received
	 */
	public Long getDataPathID() {
		if (this.featuresReply == null)
			return null;
		else
			return featuresReply.getDatapathId();
	}

	public void getLinks(List<LinkAdvertisement> links) {
		// TODO Auto-generated method stub
	}

	/**
	 * @return the featuresReply
	 */
	public FVFeaturesReply getFeaturesReply() {
		return featuresReply;
	}

	/**
	 * @param featuresReply
	 *            the featuresReply to set
	 */
	public void setFeaturesReply(FVFeaturesReply featuresReply) {
		FVLog.log(LogLevel.DEBUG, this, "got featuresReply: " + featuresReply);
		boolean wasConnected = this.isConnected();
		this.featuresReply = featuresReply;
		if (isConnected() && !wasConnected)
			this.doJustConnected();
	}

	/**
	 * @return the descriptionStatistics
	 */
	public FVDescriptionStatistics getDescriptionStatistics() {
		return descriptionStatistics;
	}

	/**
	 * @param descriptionStatistics
	 *            the descriptionStatistics to set
	 */
	public void setDescriptionStatistics(
			FVDescriptionStatistics descriptionStatistics) {
		boolean wasConnected = this.isConnected();
		FVLog.log(LogLevel.DEBUG, this, "got descStats: "
				+ descriptionStatistics);
		this.descriptionStatistics = descriptionStatistics;
		if (isConnected() && !wasConnected)
			this.doJustConnected();
	}

	private void doJustConnected() {
		this.name = "topoDpid="
				+ HexString.toHexString(this.featuresReply.getDatapathId());
		// FIXME: just one time; do more often
		for (OFPhysicalPort port : featuresReply.getPorts())
			sendLLDP(port);
	}

	private void sendLLDP(OFPhysicalPort port) {
		OFPacketOut packetOut = (OFPacketOut) this.fvMessageFactory
				.getMessage(OFType.PACKET_OUT);
		List<OFAction> actionsList = new LinkedList<OFAction>();
		OFActionOutput out = (OFActionOutput) this.fvMessageFactory
				.getAction(OFActionType.OUTPUT);
		out.setPort(port.getPortNumber());
		actionsList.add(out);
		packetOut.setActions(actionsList);
		short alen = FVMessageUtil.countActionsLen(actionsList);
		byte[] lldp = makeLLDP(port.getPortNumber(), port.getHardwareAddress());
		packetOut.setActionsLength(alen);
		packetOut.setPacketData(lldp);
		packetOut
				.setLength((short) (OFPacketOut.MINIMUM_LENGTH + alen + lldp.length));
		this.msgStream.write(packetOut);
	}

	private byte[] makeLLDP(short portNumber, byte[] hardwareAddress) {
		// TODO steal a real LLDP implementation
		byte[] buf = new byte[24];
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.put(LLDPUtil.LLDP_MULTICAST); // dst addr
		bb.put(hardwareAddress); // src addr
		bb.putShort(LLDPUtil.ETHER_LLDP);
		bb.putLong(this.featuresReply.getDatapathId());
		bb.putShort(portNumber);
		return buf;
	}
}
