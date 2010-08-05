/**
 * 
 */
package org.flowvisor.ofswitch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * Divide ports on a switch in to "slowPorts" and "fastPorts" send topology
 * discovery probes the fastPorts more often then slowports if we get an lldp
 * message on a port, make it a fast port
 * 
 * @author capveg
 * 
 */
public class TopologyConnection implements FVEventHandler {

	private static final int LLDPLen = 128;
	TopologyController topologyController;
	FVEventLoop pollLoop;
	SocketChannel sock;
	String name;
	OFMessageAsyncStream msgStream;
	FVMessageFactory fvMessageFactory;
	FVFeaturesReply featuresReply;
	FVDescriptionStatistics descriptionStatistics;
	private boolean isShutdown;
	private final long fastProbeRate;
	private final long probesPerPeriod; // this is the safety rate: this many
	private final Set<Short> slowPorts;
	private final Set<Short> fastPorts;
	private Iterator<Short> slowIterator;
	private final Map<Short, OFPhysicalPort> phyMap;

	// probes can be dropped before a link
	// down event

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
		this.probesPerPeriod = 3;
		this.fastProbeRate = this.topologyController.getUpdatePeriod()
				/ this.getProbesPerPeriod();
		this.slowPorts = new HashSet<Short>();
		this.fastPorts = new HashSet<Short>();
		this.phyMap = new HashMap<Short, OFPhysicalPort>();
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
		if (this.isShutdown)
			return; // ignore events if we've done the teardown
		if (e instanceof FVIOEvent)
			handleIOEvent((FVIOEvent) e);
		else if (e instanceof FVTimerEvent)
			handleTimerEvent((FVTimerEvent) e);
		else
			throw new UnhandledEvent(e);
	}

	/*
	 * Handle a timer event
	 * 
	 * On each timer event:<br>
	 * 
	 * <ul> <li> send a probe to each fast Port
	 * 
	 * <li> send a probe to the next slow port
	 * 
	 * <li> reschedule timer
	 * 
	 * </ul>
	 */
	private void handleTimerEvent(FVTimerEvent e) {
		FVLog.log(LogLevel.DEBUG, this, "sending probes");
		// send a probe per fast port
		for (Iterator<Short> fastIterator = this.fastPorts.iterator(); fastIterator
				.hasNext();) {
			Short port = fastIterator.next();
			FVLog.log(LogLevel.DEBUG, this, "sending fast probe to port "
					+ port);
			sendLLDP(this.phyMap.get(port));
		}
		// send a probe for the next slow port
		if (this.slowPorts.size() > 0) {
			if (!this.slowIterator.hasNext())
				this.slowIterator = this.slowPorts.iterator();
			if (this.slowIterator.hasNext()) {
				short port = this.slowIterator.next();
				sendLLDP(this.phyMap.get(port));
				FVLog.log(LogLevel.DEBUG, this, "sending slow probe to port "
						+ port);
			}

		}
		// reschedule timer
		this.pollLoop.addTimer(new FVTimerEvent(System.currentTimeMillis()
				+ this.fastProbeRate, this, this, null));
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
			this.isShutdown = true;
			this.topologyController.disconnect(this);
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
		FVLog.log(LogLevel.INFO, this, "starting topo discover: fasttimer = "
				+ this.fastProbeRate);
		// just one time; the timer event will cause them more often
		List<OFPhysicalPort> ports = featuresReply.getPorts();
		if (ports.size() < 1)
			FVLog.log(LogLevel.WARN, this, "got switch with no ports!?!");

		for (OFPhysicalPort port : ports) {
			FVLog.log(LogLevel.DEBUG, this, "sending init probe to port "
					+ port.getPortNumber());
			sendLLDP(port);
			this.slowPorts.add(Short.valueOf(port.getPortNumber()));
			this.phyMap.put(Short.valueOf(port.getPortNumber()), port);
		}
		this.slowIterator = this.slowPorts.iterator();
		// schedule timer
		this.pollLoop.addTimer(new FVTimerEvent(System.currentTimeMillis()
				+ this.fastProbeRate, this, this, null));
	}

	private void sendLLDP(OFPhysicalPort port) {
		OFPacketOut packetOut = (OFPacketOut) this.fvMessageFactory
				.getMessage(OFType.PACKET_OUT);
		packetOut.setBufferId(-1);
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
		int size = LLDPLen; // needs to be some minsize to avoid ethernet
		// problems
		byte[] buf = new byte[size];
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.put(LLDPUtil.LLDP_MULTICAST); // dst addr
		bb.put(hardwareAddress); // src addr
		bb.putShort(LLDPUtil.ETHER_LLDP);
		bb.putLong(this.featuresReply.getDatapathId());
		bb.putShort(portNumber);
		while (bb.position() <= (size - 4))
			bb.putInt(0xcafebabe); // fill with well known padding
		return buf;
	}

	/**
	 * @return the topologyController
	 */
	public TopologyController getTopologyController() {
		return topologyController;
	}

	/**
	 * @param topologyController
	 *            the topologyController to set
	 */
	public void setTopologyController(TopologyController topologyController) {
		this.topologyController = topologyController;
	}

	static public DPIDandPort parseLLDP(byte[] packet) {
		if (packet == null || packet.length != LLDPLen)
			return null; // invalid lldp
		ByteBuffer bb = ByteBuffer.wrap(packet);
		byte[] dst = new byte[6];
		bb.get(dst);
		if (!Arrays.equals(dst, LLDPUtil.LLDP_MULTICAST))
			return null;
		bb.position(12);
		short etherType = bb.getShort();
		if (etherType != LLDPUtil.ETHER_LLDP)
			return null;
		bb.position(14);
		long dpid = bb.getLong();
		short port = bb.getShort();
		return new DPIDandPort(dpid, port);
	}

	public long getProbesPerPeriod() {
		return probesPerPeriod;
	}

	synchronized void signalPortTimeout(short port) {
		Short sPort = Short.valueOf(port);
		if (this.fastPorts.contains(sPort)) {
			FVLog.log(LogLevel.MOBUG, this, "setting fast port to slow: "
					+ port);
			this.fastPorts.remove(sPort);
			this.slowPorts.add(sPort);
		} else if (!this.slowPorts.contains(sPort)) {
			FVLog.log(LogLevel.WARN, this,
					"got signalPortTimeout for non-existant port: " + port);
		}
	}

	public synchronized void signalFastPort(short port) {
		Short sPort = Short.valueOf(port);
		if (this.slowPorts.contains(sPort)) {
			FVLog.log(LogLevel.DEBUG, this, "setting slow port to fast: "
					+ port);
			this.slowPorts.remove(sPort);
			this.slowIterator = this.slowPorts.iterator();
			this.fastPorts.add(sPort);
		} else if (!this.fastPorts.contains(sPort)) {
			FVLog.log(LogLevel.WARN, this,
					"got signalFastPort for non-existant port: " + port);
		}
	}
}
