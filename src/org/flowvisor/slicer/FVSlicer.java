/**
 *
 */
package org.flowvisor.slicer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.FVSendMsg;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.ConfigUpdateEvent;
import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.events.OFKeepAlive;
import org.flowvisor.events.TearDownEvent;
import org.flowvisor.exceptions.BufferFull;
import org.flowvisor.exceptions.MalformedOFMessage;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.flows.FlowDB;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.LinearFlowDB;
import org.flowvisor.flows.NoopFlowDB;
import org.flowvisor.io.FVMessageAsyncStream;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.log.SendRecvDropStats;
import org.flowvisor.log.SendRecvDropStats.FVStatsType;
import org.flowvisor.message.FVMessageFactory;
import org.flowvisor.message.SanityCheckable;
import org.flowvisor.message.Slicable;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;

/**
 * @author capveg
 * 
 */
public class FVSlicer implements FVEventHandler, FVSendMsg {

	public static final int MessagesPerRead = 50; // for performance tuning
	String sliceName;
	FVClassifier fvClassifier;
	FVEventLoop loop;
	SocketChannel sock;
	String hostname;
	int reconnectSeconds;
	final int maxReconnectSeconds = 15;
	int port; // the tcp port of our controller
	boolean isConnected;
	FVMessageAsyncStream msgStream;
	short missSendLength;
	boolean allowAllPorts;
	FlowMap localFlowSpace;
	boolean isShutdown;
	OFKeepAlive keepAlive;
	SendRecvDropStats stats;
	FlowDB realFlowDB; // after slicing expansion; what the slice gets
	FlowDB virtualFlowDB; // before slicing expansion; what the slice thinks
	Map<Short, Boolean> allowedPorts; // ports in this slice and whether they

	// get OFPP_FLOOD'd

	public FVSlicer(FVEventLoop loop, FVClassifier fvClassifier,
			String sliceName) {
		this.loop = loop;
		this.fvClassifier = fvClassifier;
		this.sliceName = sliceName;
		this.isConnected = false;
		this.msgStream = null;
		this.missSendLength = 128; // openflow default (?) findout... TODO
		this.allowAllPorts = false;
		this.reconnectSeconds = 0;
		this.isShutdown = false;
		this.allowedPorts = new HashMap<Short, Boolean>();
		this.stats = SendRecvDropStats.createSharedStats(sliceName);
		try {
			if (FVConfig.getBoolean(FVConfig.FLOW_TRACKING)) {
				this.realFlowDB = new LinearFlowDB(this);
				this.virtualFlowDB = new LinearFlowDB(this);
			} else {
				this.realFlowDB = new NoopFlowDB();
				this.virtualFlowDB = new NoopFlowDB();
			}
		} catch (ConfigError e) {
			// default to flow_tracking == off
			this.realFlowDB = new NoopFlowDB();
			this.virtualFlowDB = new NoopFlowDB();
		}
	}

	public void init() {
		FVLog.log(LogLevel.DEBUG, this, "initializing new FVSlicer");
		String sliceBase = FVConfig.SLICES + FVConfig.FS + this.sliceName;
		// snag controller info from config
		try {
			hostname = FVConfig.getString(sliceBase + FVConfig.FS
					+ FVConfig.SLICE_CONTROLLER_HOSTNAME);
			FVConfig.watch(this, sliceBase + FVConfig.FS
					+ FVConfig.SLICE_CONTROLLER_HOSTNAME);
			port = FVConfig.getInt(sliceBase + FVConfig.FS
					+ FVConfig.SLICE_CONTROLLER_PORT);
			FVConfig.watch(this, sliceBase + FVConfig.FS
					+ FVConfig.SLICE_CONTROLLER_PORT);
		} catch (ConfigError e) {
			FVLog.log(LogLevel.CRIT, this, "ignoring slice ", sliceName,
					" malformed slice definition: ", e);
			this.tearDown();
			return;
		}
		this.updatePortList();
		this.reconnect();
		this.keepAlive = new OFKeepAlive(this, this, loop);
		this.keepAlive.scheduleNextCheck();
	}

	private void updatePortList() {
		synchronized (FVConfig.class) {
			// update our local copy
			this.localFlowSpace = this.fvClassifier.getSwitchFlowMap().clone();
		}
		Set<Short> ports = FlowSpaceUtil.getPortsBySlice(this.fvClassifier
				.getSwitchInfo().getDatapathId(), this.sliceName,
				this.localFlowSpace);
		if (ports.contains(OFPort.OFPP_ALL.getValue())) {
			// this switch has access to ALL PORTS; feed them in from the
			// features request
			ports.clear(); // remove the OFPP_ALL virtual port
			this.allowAllPorts = true;
			for (OFPhysicalPort phyPort : this.fvClassifier.getSwitchInfo()
					.getPorts())
				ports.add(phyPort.getPortNumber());
		}
		for (Short port : ports) {
			if (!allowedPorts.keySet().contains(port)) {
				FVLog.log(LogLevel.DEBUG, this, "adding access to port ", port);
				allowedPorts.put(port, Boolean.TRUE);
			}
		}
		for (Iterator<Short> it = allowedPorts.keySet().iterator(); it
				.hasNext();) {
			Short port = it.next();
			if (!ports.contains(port)) {
				FVLog.log(LogLevel.DEBUG, this, "removing access to port ",
						port);
				it.remove();
			}
		}
	}

	/**
	 * Return the list of ports in this slice on this switch
	 * 
	 * @return
	 */
	public Set<Short> getPorts() {
		return this.allowedPorts.keySet();
	}

	/**
	 * Return the list of ports that have flooding enabled for OFPP_FLOOD
	 * 
	 * @return
	 */
	public Set<Short> getFloodPorts() {
		Set<Short> floodPorts = new LinkedHashSet<Short>();
		for (Short port : this.allowedPorts.keySet())
			if (this.allowedPorts.get(port))
				floodPorts.add(port);
		return floodPorts;
	}

	public boolean isAllowAllPorts() {
		return allowAllPorts;
	}

	/**
	 * @return the missSendLength
	 */
	public short getMissSendLength() {
		return missSendLength;
	}

	/**
	 * @param missSendLength
	 *            the missSendLength to set
	 */
	public void setMissSendLength(short missSendLength) {
		this.missSendLength = missSendLength;
	}

	/**
	 * Set the OFPP_FLOOD flag for this port silently fail if this port is not
	 * in the slice
	 * 
	 * @param port
	 * @param status
	 */

	public void setFloodPortStatus(Short port, Boolean status) {
		if (this.allowedPorts.containsKey(port))
			this.allowedPorts.put(port, status);
	}

	/**
	 * Is this port in this slice on this switch?
	 * 
	 * @param port
	 * @return true is yes, false is no.. durh
	 */
	public boolean portInSlice(Short port) {
		return (this.allowAllPorts || this.allowedPorts.containsKey(port));
	}

	@Override
	public void sendMsg(OFMessage msg, FVSendMsg from) {
		if (this.msgStream != null) {
			FVLog.log(LogLevel.DEBUG, this, "send to controller: ", msg);
			try {
				this.msgStream.testAndWrite(msg);
			} catch (BufferFull e) {
				FVLog.log(LogLevel.CRIT, this,
						"buffer full: tearing down: got ", e,
						": resetting connection");
				this.reconnectLater();
			} catch (MalformedOFMessage e) {
				this.stats.increment(FVStatsType.DROP, from, msg);
				FVLog.log(LogLevel.CRIT, this, "BUG: ", e);
			} catch (IOException e) {
				FVLog.log(LogLevel.WARN, this, "reconnection; got IO error: ",
						e);
				this.reconnectLater();
			}
		} else {
			this.stats.increment(FVStatsType.DROP, from, msg);
			FVLog.log(LogLevel.WARN, this,
					"dropping msg: controller not connected: ", msg);
		}
	}

	@Override
	public void dropMsg(OFMessage msg, FVSendMsg from) {
		this.stats.increment(FVStatsType.DROP, from, msg);
	}

	@Override
	public boolean needsConnect() {
		return !this.isConnected; // want connect events if we're not connected
	}

	@Override
	public boolean needsRead() {
		return this.isConnected; // want read events if we are connected
	}

	@Override
	public boolean needsWrite() {
		if (this.msgStream == null) // want write events if msgStream wants them
			return false;
		return this.msgStream.needsFlush();
	}

	@Override
	public boolean needsAccept() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#getName()
	 */
	@Override
	public String getName() {
		return new StringBuilder("slicer_").append(this.sliceName).append("_")
				.append(fvClassifier.getSwitchName()).toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#getThreadContext()
	 */
	@Override
	public long getThreadContext() {
		// TODO Auto-generated method stub
		return loop.getThreadContext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.events.FVEventHandler#tearDown()
	 */
	@Override
	public void tearDown() {
		closeDown(true);
	}

	public void closeDown(boolean unregisterClassifier) {
		FVLog.log(LogLevel.DEBUG, this, "tearing down");
		this.isShutdown = true;
		this.loop.unregister(this.sock, this);
		if (this.sock != null) {
			try {
				this.sock.close(); // FIXME will this also cancel() the key in
				// the event loop?
			} catch (IOException e) {
				// ignore if error... we're shutting down already
			}
		}
		// tell the classifier to forget about us
		if (unregisterClassifier)
			fvClassifier.tearDownSlice(this.sliceName);

		this.msgStream = null; // force this to GC, in case we have a memleak on
		// "this"

		String sliceBase = FVConfig.SLICES + FVConfig.FS + this.sliceName;
		FVConfig.unwatch(this, sliceBase + FVConfig.FS
				+ FVConfig.SLICE_CONTROLLER_HOSTNAME);
		FVConfig.unwatch(this, sliceBase + FVConfig.FS
				+ FVConfig.SLICE_CONTROLLER_PORT);
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
		if (isShutdown) {
			FVLog.log(LogLevel.WARN, this, "is shutdown; ignoring: " + e);
			return; // don't process any events after shutdown
		}
		if (e instanceof FVIOEvent)
			handleIOEvent((FVIOEvent) e);
		else if (e instanceof OFKeepAlive)
			handleKeepAlive(e);
		else if (e instanceof ConfigUpdateEvent)
			updateConfig((ConfigUpdateEvent) e);
		else if (e instanceof ReconnectEvent)
			this.reconnect();
		else if (e instanceof TearDownEvent)
			this.tearDown();
		else
			throw new UnhandledEvent(e);
	}

	private void handleKeepAlive(FVEvent e) {
		if (!this.keepAlive.isAlive()) {
			FVLog.log(LogLevel.WARN, this,
					"keepAlive timeout; trying to reconnnect later");
			try {
				this.sock.close();
			} catch (IOException e1) {
				FVLog.log(LogLevel.WARN, this,
						"ignoring error while closing socket: ", e1);
			}
			this.reconnectLater();
			return;
		}
		this.keepAlive.sendPing();
		this.keepAlive.scheduleNextCheck();
	}

	public void registerPong() {
		this.keepAlive.registerPong();
	}

	/**
	 * We got a signal that something in the config changed
	 * 
	 * @param e
	 */

	private void updateConfig(ConfigUpdateEvent e) {
		String whatChanged = e.getConfig();
		if (whatChanged.equals(FVConfig.FLOWSPACE))
			updateFlowSpaceConfig(e);
		else
			FVLog.log(LogLevel.WARN, this,
					"ignoring unhandled/implemented config update:", e);
	}

	/**
	 * The FlowSpace just changed; update all cached dependencies
	 * 
	 * @param e
	 */

	private void updateFlowSpaceConfig(ConfigUpdateEvent e) {
		updatePortList();
		// FIXME: implement compare of old vs. new flowspace
		// and remove flow entries that don't fit the difference
		FVLog.log(LogLevel.CRIT, this, "FIXME: need to flush old flow entries");
	}

	private void reconnect() {
		FVLog.log(LogLevel.DEBUG, this,
				"trying to connect to " + this.hostname, ":", this.port);
		// reset our state to unconnected (might be a NOOP)
		this.isConnected = false;
		this.msgStream = null;
		// try to connect socket to controller
		try {
			if (this.sock != null)
				this.sock.close();
			this.sock = SocketChannel.open();
			sock.configureBlocking(false); // set to non-blocking
			InetSocketAddress addr = new InetSocketAddress(hostname, port);
			if (addr.isUnresolved()) {
				FVLog.log(LogLevel.INFO, this,
						"retrying: failed to resolve hostname: ", hostname);
				this.reconnectLater();
				return;
			}
			this.isConnected = this.sock.connect(addr); // try to connect
			// register into event loop
			this.loop.register(this.sock, SelectionKey.OP_CONNECT, this);
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, this,
					"Trying to reconnect; trying later; got : ", e);
			this.reconnectLater();
		}

	}

	private void handleIOEvent(FVIOEvent e) {
		if (!this.isConnected) {
			try {
				if (!this.sock.finishConnect())
					return; // not done yet

			} catch (IOException e1) {
				FVLog.log(LogLevel.DEBUG, this, "retrying connection in ",
						this.reconnectSeconds, " seconds; got: ", e1);
				this.reconnectLater();
				return;
			}
			FVLog.log(LogLevel.DEBUG, this, "connected");
			this.isConnected = true;
			this.reconnectSeconds = 0;
			try {
				msgStream = new FVMessageAsyncStream(this.sock,
						new FVMessageFactory(), this, this.stats);
			} catch (IOException e1) {
				FVLog.log(
						LogLevel.WARN,
						this,
						"Trying again later; while creating OFMessageAsyncStream, got: ",
						e1);
				this.reconnectLater();
				return;
			}
			sendMsg(new OFHello(), this); // send initial handshake
		}
		try {
			if (msgStream.needsFlush()) // flush any pending messages
				msgStream.flush();
			List<OFMessage> msgs = this.msgStream
					.read(FVSlicer.MessagesPerRead); // read any new
			// messages
			if (msgs == null)
				throw new IOException("got null from read()");
			for (OFMessage msg : msgs) {
				FVLog.log(LogLevel.DEBUG, this, "recv from controller: ", msg);
				this.stats.increment(FVStatsType.SEND, this, msg);
				if ((msg instanceof SanityCheckable)
						&& (!((SanityCheckable) msg).isSane())) {
					FVLog.log(LogLevel.CRIT, this,
							"msg failed sanity check; dropping: " + msg);
					continue;
				}
				if (msg instanceof Slicable) {
					((Slicable) msg).sliceFromController(fvClassifier, this);
					// mark this channel as still alive
					this.keepAlive.registerPong();
				} else
					FVLog.log(LogLevel.CRIT, this,
							"dropping msg that doesn't implement classify: ",
							msg);
			}
		} catch (IOException e1) {
			FVLog.log(LogLevel.WARN, this,
					"got i/o error; tearing down and reconnecting: ", e1);
			reconnect();
		} catch (Exception e2) {
			e2.printStackTrace();
			FVLog.log(LogLevel.ALERT, this,
					"got unknown error; tearing down and reconnecting: ", e2);
			reconnect();
		}
		// no need to setup for next select; done in eventloop
	}

	private void reconnectLater() {
		if (this.sock != null)
			try {
				this.sock.close();
			} catch (IOException e) {
				FVLog.log(LogLevel.WARN, this,
						"ignoring error closing socket: ", e);
			}
		// exponential back off
		this.reconnectSeconds = Math.min(2 * this.reconnectSeconds + 1,
				this.maxReconnectSeconds);
		this.loop.addTimer(new ReconnectEvent(this.reconnectSeconds, this));
	}

	public String getSliceName() {
		return this.sliceName;
	}

	public boolean getFloodPortStatus(short port) {
		return this.allowedPorts.get(port);
	}

	public FlowMap getFlowSpace() {
		return this.localFlowSpace;
	}

	@Override
	public String getConnectionName() {
		if (isConnected)
			return FlowSpaceUtil.connectionToString(sock);
		else
			return "NONE (retry in " + this.reconnectSeconds
					+ " seconds: max + " + this.maxReconnectSeconds + ")";
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	@Override
	public SendRecvDropStats getStats() {
		return stats;
	}

	public FlowDB getRealFlowDB() {
		return realFlowDB;
	}

	public FlowDB getVirtualFlowDB() {
		return virtualFlowDB;
	}

}
