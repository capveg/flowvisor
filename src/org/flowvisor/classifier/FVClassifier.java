package org.flowvisor.classifier;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowvisor.config.FVConfig;
import org.flowvisor.events.ConfigUpdateEvent;
import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.Classifiable;
import org.flowvisor.message.FVError;
import org.flowvisor.message.FVMessageFactory;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFeaturesRequest;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFError.OFHelloFailedCode;

/**
 * Map OF messages from the switch to the appropriate slice
 * 
 * Also handles all of the switch-specific but slice-general state and
 * rewriting.
 * 
 * @author capveg
 * 
 */

public class FVClassifier implements FVEventHandler {

	FVEventLoop loop;
	SocketChannel sock;
	String switchName;
	boolean doneID;
	OFMessageAsyncStream msgStream;
	OFFeaturesReply switchInfo;
	Map<String, FVSlicer> slicerMap;
	XidTranslator xidTranslator;
	short missSendLength;
	FlowMap switchFlowMap;
	private boolean shutdown;
	private final FVMessageFactory factory;

	public FVClassifier(FVEventLoop loop, SocketChannel sock) {
		this.loop = loop;
		this.switchName = "unidentified:" + sock.toString();
		this.factory = new FVMessageFactory();
		try {
			this.msgStream = new OFMessageAsyncStream(sock, this.factory);
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, this, "IOException in constructor!");
			e.printStackTrace();
		}
		this.sock = sock;
		this.switchInfo = null;
		this.doneID = false;
		this.slicerMap = new HashMap<String, FVSlicer>();
		this.xidTranslator = new XidTranslator();
		this.missSendLength = 128;
		this.switchFlowMap = null;
	}

	public short getMissSendLength() {
		return missSendLength;
	}

	public void setMissSendLength(short missSendLength) {
		this.missSendLength = missSendLength;
	}

	@Override
	public boolean needsConnect() {
		return false; // never want connect events
	}

	@Override
	public boolean needsRead() {
		return true; // always want read events
	}

	@Override
	public boolean needsWrite() {
		if (this.msgStream == null)
			return false;
		return this.msgStream.needsFlush();
	}

	@Override
	public boolean needsAccept() {
		return false;
	}

	public OFMessageAsyncStream getMsgStream() {
		return msgStream;
	}

	public void setMsgStream(OFMessageAsyncStream msgStream) {
		this.msgStream = msgStream;
	}

	public OFFeaturesReply getSwitchInfo() {
		return switchInfo;
	}

	public void setSwitchInfo(OFFeaturesReply switchInfo) {
		this.switchInfo = switchInfo;
	}

	public FVSlicer getSlicerByName(String sliceName) {
		synchronized (slicerMap) {
			return slicerMap.get(sliceName);
		}
	}

	public XidTranslator getXidTranslator() {
		return xidTranslator;
	}

	public void setXidTranslator(XidTranslator xidTranslator) {
		this.xidTranslator = xidTranslator;
	}

	/**
	 * on init, send HELLO, delete all flow entries, and send features request
	 * 
	 * @throws IOException
	 */

	public void init() throws IOException {
		// send initial handshake
		msgStream.write(new OFHello());
		// delete all entries in the flowtable
		OFMatch match = new OFMatch();
		match.setWildcards(OFMatch.OFPFW_ALL);
		OFFlowMod fm = new OFFlowMod();
		fm.setMatch(match);
		fm.setCommand(OFFlowMod.OFPFC_DELETE);
		fm.setOutPort(OFPort.OFPP_NONE);
		fm.setBufferId(0xffffffff); // buffer to NONE
		msgStream.write(fm);
		// request the switch's features
		msgStream.write(new OFFeaturesRequest());
		msgStream.flush();
		int ops = SelectionKey.OP_READ;
		if (msgStream.needsFlush())
			ops |= SelectionKey.OP_WRITE;
		// this now calls FlowVisor.addHandler()
		loop.register(sock, ops, this);
	}

	@Override
	public String getName() {
		return "classifier-" + switchName;
	}

	@Override
	public long getThreadContext() {
		return loop.getThreadContext();
	}

	@Override
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		if (this.shutdown) {
			FVLog.log(LogLevel.WARN, this, "is shutdown: ignoring: " + e);
			return;
		}
		if (Thread.currentThread().getId() != this.getThreadContext()) {
			// this event was sent from a different thread context
			loop.queueEvent(e); // queue event
			return; // and process later
		}
		if (e instanceof FVIOEvent)
			handleIOEvent((FVIOEvent) e);
		else if (e instanceof ConfigUpdateEvent)
			updateConfig((ConfigUpdateEvent) e);
		else
			throw new UnhandledEvent(e);
	}

	/**
	 * Something in the config has changed since we only register for FlowSpace
	 * changes, must be a new FlowSpace
	 * 
	 * @param e
	 */
	private void updateConfig(ConfigUpdateEvent e) {
		FVLog.log(LogLevel.DEBUG, this, "got update: " + e);
		// update ourselves first
		connectToControllers(); // re-figure out who we should connect to
		// then tell everyone who depends on us (causality important :-)
		for (FVSlicer fvSlicer : slicerMap.values())
			try {
				fvSlicer.handleEvent(e);
			} catch (UnhandledEvent e1) {
			} // don't worry if they don't handle event
	}

	void handleIOEvent(FVIOEvent e) {
		int ops = e.getSelectionKey().readyOps();

		try {
			// read stuff, if need be
			if ((ops & SelectionKey.OP_READ) != 0) {
				List<OFMessage> newMsgs = msgStream.read();
				if (newMsgs != null) {
					for (OFMessage m : newMsgs) {
						if (m == null) {
							FVLog
									.log(
											LogLevel.ALERT,
											this,
											"got an unparsable OF Message "
													+ "(msgStream.read() returned a null):"
													+ "trying to ignore it");
							continue;
						}
						FVLog.log(LogLevel.DEBUG, this, "read " + m);
						if (switchInfo != null)
							classifyOFMessage(m);
						else
							handleOFMessage_unidenitified(m);
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
		// no need to setup for next select; done in eventloop
	}

	/**
	 * Close all slice connections and cleanup
	 */
	@Override
	public void tearDown() {
		FVLog.log(LogLevel.WARN, this, "tearing down");
		this.loop.unregister(this.sock, this);
		this.shutdown = true;
		try {
			this.sock.close();
			// shutdown each of the connections to the controllers
			Map<String, FVSlicer> tmpMap = slicerMap;
			slicerMap = null; // to prevent tearDown(slice) corruption
			for (FVSlicer fvSlicer : tmpMap.values())
				fvSlicer.closeDown(false);
		} catch (IOException e) {
			// Silently ignore... already tearing down
		}
	}

	/**
	 * Main function Pass this message on to the appropriate Slicer as defined
	 * by XID, FlowSpace, config, etc.
	 * 
	 * @param m
	 */
	private void classifyOFMessage(OFMessage msg) {
		FVLog.log(LogLevel.DEBUG, this, "received from switch: " + msg);
		((Classifiable) msg).classifyFromSwitch(this); // msg specific handling
	}

	/**
	 * State machine for switches before we know which switch it is
	 * 
	 * Wait for FEATURES_REPLY; ignore everything else
	 * 
	 * @param m
	 *            incoming message
	 */
	private void handleOFMessage_unidenitified(OFMessage m) {
		switch (m.getType()) {
		case HELLO: // aleady sent our hello; just NOOP here
			if (m.getVersion() != OFMessage.OFP_VERSION) {
				FVLog.log(LogLevel.WARN, this,
						"Mismatched version from switch " + sock + " Got: "
								+ m.getVersion() + " Wanted: "
								+ OFMessage.OFP_VERSION);
				FVError fvError = (FVError) this.factory
						.getMessage(OFType.ERROR);
				fvError.setErrorCode(OFHelloFailedCode.OFPHFC_INCOMPATIBLE);
				fvError.setVersion(m.getVersion());
				String errmsg = "we only support version "
						+ Integer.toHexString(OFMessage.OFP_VERSION)
						+ " and you are not it";
				fvError.setAsciiError(errmsg.getBytes());
				fvError.setLength((short) (FVError.MINIMUM_LENGTH + errmsg
						.length()));
				this.sendMsg(fvError);
				tearDown();
			}
			break;
		case ECHO_REQUEST:
			OFMessage echo_reply = new OFEchoReply();
			echo_reply.setXid(m.getXid());
			msgStream.write(echo_reply);
			break;
		case FEATURES_REPLY:
			switchInfo = (OFFeaturesReply) m;
			/*
			 * OFStatisticsRequest stats = new OFStatisticsRequest();
			 * stats.setStatisticType(OFStatisticsType.DESC);
			 */
			switchName = "dpid:"
					+ String.format("%1$x", switchInfo.getDatapathId());
			FVLog.log(LogLevel.INFO, this, "identified switch as " + switchName
					+ " on " + this.sock);
			FVConfig.watch(this, FVConfig.FLOWSPACE); // register for FS updates
			this.connectToControllers(); // connect to controllers
			doneID = true;
			break;
		default:
			// FIXME add logging
			FVLog.log(LogLevel.WARN, this, "Got unknown message type " + m
					+ " to unidentified switch");
		}
	}

	/**
	 * Figure out which slices have access to the switch and spawn a Slicer
	 * EventHandler for each of them. Also, close the connection to any slice
	 * that is no longer listed
	 * 
	 * Also make a connection for the topology discovery daemon here if
	 * configured
	 * 
	 * Assumes The switch is already been identified;
	 * 
	 */
	private void connectToControllers() {

		this.switchFlowMap = FlowSpaceUtil.getSubFlowMap(this.switchInfo
				.getDatapathId());
		// this.switchFlowMap = FVConfig.getFlowSpaceFlowMap();
		Set<String> newSlices = FlowSpaceUtil.getSlicesByDPID(
				this.switchFlowMap, this.switchInfo.getDatapathId());
		StringBuffer strbuf = new StringBuffer();
		for (String sliceName : newSlices) {
			if (strbuf.length() > 0) // prune the last
				strbuf.append(',');
			strbuf.append(sliceName);
		}

		FVLog.log(LogLevel.DEBUG, this, "slices with access="
				+ strbuf.toString());
		// foreach slice, make sure it has access to this switch
		for (String sliceName : newSlices) {
			if (slicerMap == null)
				throw new NullPointerException("slicerMap is null!?");
			if (!slicerMap.containsKey(sliceName)) {
				FVLog.log(LogLevel.INFO, this,
						"making new connection to slice " + sliceName);
				FVSlicer newSlicer = new FVSlicer(this.loop, this, sliceName);
				slicerMap.put(sliceName, newSlicer); // create new slicer in
				// this same EventLoop
				newSlicer.init(); // and start it up
			}
		}

		// foreach slice with previous access, make sure it still has access
		List<String> deletelist = new LinkedList<String>();
		for (String sliceName : slicerMap.keySet()) {
			if (!newSlices.contains(sliceName)) {
				// this slice no longer has access to this switch
				FVLog.log(LogLevel.INFO, this,
						"disconnecting: removed from FlowSpace: " + sliceName);
				slicerMap.get(sliceName).closeDown(false);
				deletelist.add(sliceName);
			}
		}
		// delete anything we marked in prev pass
		// should be able to do this in one loop, but can't
		// seem to iterate over a Map's keys and del inline
		for (String deleteSlice : deletelist)
			slicerMap.remove(deleteSlice);
	}

	public FlowMap getSwitchFlowMap() {
		return switchFlowMap;
	}

	public void setSwitchFlowMap(FlowMap switchFlowMap) {
		this.switchFlowMap = switchFlowMap;
	}

	/**
	 * Called by FVSlicer to tell us to forget about them
	 * 
	 * @param sliceName
	 */
	public void tearDownSlice(String sliceName) {
		if (slicerMap != null) {
			slicerMap.remove(sliceName);
			FVLog.log(LogLevel.DEBUG, this, "tore down slice " + sliceName
					+ " on request");
		}
	}

	public String getSwitchName() {
		return this.switchName;
	}

	public String getRemoteIP() {
		return this.sock.socket().getRemoteSocketAddress().toString();
	}

	/**
	 * @return This switch's DPID
	 */
	public long getDPID() {
		return this.switchInfo.getDatapathId();
	}

	/**
	 * Send a message to the switch connected to this classifier
	 * 
	 * @param msg
	 *            OFMessage
	 */

	public void sendMsg(OFMessage msg) {
		if (this.msgStream != null) {
			FVLog.log(LogLevel.DEBUG, this, "send to switch:" + msg);
			this.msgStream.write(msg);
		} else
			FVLog.log(LogLevel.WARN, this, "dropping msg: no connection: "
					+ msg);
	}

	public Collection<FVSlicer> getSlicers() {
		// TODO: figure out if this is a copy and could have SYNCH issues
		return slicerMap.values();
	}
}
