/**
 * 
 */
package org.flowvisor.slicer;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Set;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.message.*;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMessage;

/**
 * @author capveg
 *
 */
public class FVSlicer implements FVEventHandler {


	String sliceName;
	FVClassifier fvClassifier;
	FVEventLoop loop;
	SocketChannel sock;
	String hostname;
	int port;							// the tcp port of our controller
	boolean isConnected;
	OFMessageAsyncStream msgStream;
	int missSendLength;
	Set<Short> ports;  					// the list of ports we're allowed to access on this switch
 	
	public FVSlicer(FVEventLoop loop, FVClassifier fvClassifier, String sliceName) {
		this.loop = loop;
		this.fvClassifier = fvClassifier;
		this.sliceName = sliceName;
		this.isConnected = false;
		this.msgStream = null;
		this.missSendLength=128;		// openflow default (?) findout...  TODO
		this.ports = null;
	}

	public void init() {
		FVLog.log(LogLevel.DEBUG, this, "initializing new FVSlicer");
		String sliceBase = FVConfig.SLICES + "." + this.sliceName;
		// snag controller info from config
		try {
			hostname = FVConfig.getString(sliceBase+ 
					"." + FVConfig.SLICE_CONTROLLER_HOSTNAME);
			FVConfig.watch(this, sliceBase+ 
					"." + FVConfig.SLICE_CONTROLLER_HOSTNAME);
			port  = FVConfig.getInt(sliceBase+ 
					"." + FVConfig.SLICE_CONTROLLER_PORT);
			FVConfig.watch(this, sliceBase+ 
					"." + FVConfig.SLICE_CONTROLLER_PORT);			
		} catch (ConfigError e) {
			FVLog.log(LogLevel.CRIT, this, "ignoring slice " + sliceName + " malformed slice definition: " + e);
			this.tearDown();
			return;
		}
		this.updatePortList();
		this.reconnect();
	}

	
	
	private void updatePortList() {
		Set<Short> ports = FlowSpaceUtil.getPortsBySlice(this.fvClassifier.getSwitchInfo().getDatapathId(), 
				this.sliceName);
		if(this.ports != null) {
			// we got a new list of ports while we are already running
			// step through and update if necessary 
			// TODO : implement!
			FVLog.log(LogLevel.CRIT, this, "dynamic ports update not yet implemented!");
		}
		this.ports = ports;	// put new ports into place		
	}

	
	public Set<Short> getPorts() {
		return ports;
	}

	public void setPorts(Set<Short> ports) {
		this.ports = ports;
	}

	public OFMessageAsyncStream getMsgStream() {
		return msgStream;
	}

	public void setMsgStream(OFMessageAsyncStream msgStream) {
		this.msgStream = msgStream;
	}

	@Override
	public boolean needsConnect() {
		return !this.isConnected;		// want connect events if we're not connected
	}

	@Override
	public boolean needsRead() {
		return this.isConnected;		// want read events if we are connected
	}

	@Override
	public boolean needsWrite() {	
		if (this.msgStream == null) 	// want write events if msgStream wants them
			return false;
		return this.msgStream.needsFlush();
	}
	
	
	
	
	@Override
	public boolean needsAccept() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#getName()
	 */
	@Override
	public String getName() {		
		return "slicer_" + this.sliceName + "_"+  fvClassifier.getSwitchName();
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#getThreadContext()
	 */
	@Override
	public long getThreadContext() {
		// TODO Auto-generated method stub
		return loop.getThreadContext();
	}


	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#tearDown()
	 */
	@Override
	public void tearDown() {
		FVLog.log(LogLevel.DEBUG, this, "tearing down");
		if (this.sock != null)
			try {
				this.sock.close();		// FIXME will this also cancel()  the key in the event loop?
			} catch (IOException e) {
				// ignore if error... we're shutting down already
			}
		fvClassifier.tearDown(this.sliceName);	// tell the classifier to forget about us
	}

	
	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#handleEvent(org.flowvisor.events.FVEvent)
	 */
	@Override
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		if (e instanceof FVIOEvent)
			handleIOEvent((FVIOEvent) e);
		else
			throw new UnhandledEvent(e);
	}

	private void reconnect() {
		FVLog.log(LogLevel.INFO, this, "trying to connect to " + 
				this.hostname + ":" + this.port);
		// reset our state to unconnected (might be a NOOP)
		this.isConnected = false;
		this.msgStream = null;
		// try to connect socket to controller
		try {
			if (this.sock != null)
				this.sock.close();
			this.sock = SocketChannel.open(); 				
			sock.configureBlocking(false);	// set to non-blocking
			this.isConnected = this.sock.connect(new InetSocketAddress(hostname, port)); // try to connect
			// register into event loop
			this.loop.register(this.sock, SelectionKey.OP_CONNECT, this);	
		} catch (IOException e) {
			// TODO:: spawn a timer event to connect again later
			FVLog.log(LogLevel.ALERT, this, "Giving up on reconnecting, got : " + e);
			tearDown();
		}
		
	}
	
	private void handleIOEvent(FVIOEvent e) {
		if (!this.isConnected) {
			try {
				if (!this.sock.finishConnect())
					return;	// not done yet
				
			} catch (IOException e1) {
				FVLog.log(LogLevel.DEBUG, this, "retrying connection got: " + e1 );
				this.reconnect();
				return;
			}
			FVLog.log(LogLevel.DEBUG, this, "connected");
			this.isConnected = true;
			try {
				msgStream = new OFMessageAsyncStream(this.sock, new FVMessageFactory());
			} catch (IOException e1) {
				FVLog.log(LogLevel.ALERT, this, "Giving up; while creating OFMessageAsyncStream, got: " 
						+ e1);
				this.tearDown();
				return;
			}
			FVLog.log(LogLevel.DEBUG, this, "sending HELLO");
			msgStream.write(new OFHello());		// send initial handshake
		}
		try {
			if(msgStream.needsFlush())	// flush any pending messages
				msgStream.flush();
			List<OFMessage> msgs = this.msgStream.read();	// read any new messages
			for(OFMessage msg: msgs)
				handleOFMsgFromController(msg);				// process new messages
		} catch(IOException e1) {
			FVLog.log(LogLevel.WARN, this, "got i/o error; tearing down and reconnecting: " + e1);
			reconnect();
		}
		// no need to setup for next select; done in eventloop			
	}

	void handleOFMsgFromController(OFMessage msg) {
		// FIXME: for now, just blindly pass on to switch
		FVLog.log(LogLevel.DEBUG, this, "recv from controller: " + msg);
		((Slicable)msg).sliceFromController(fvClassifier, this);
	}

	public void setMissSendLength(int missSendLength) {
		this.missSendLength = missSendLength;
		
	}

	public String getSliceName() {
		return this.sliceName;
	}
}
