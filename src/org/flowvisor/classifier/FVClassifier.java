package org.flowvisor.classifier;

import org.flowvisor.events.*;

import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.flows.FlowSpaceUtil;
import org.openflow.io.OFMessageAsyncStream;
import org.flowvisor.slicer.FVSlicer;
import org.flowvisor.message.*;
import org.openflow.protocol.*;
import org.flowvisor.log.*;

import java.io.IOException;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	Map<String,FVSlicer> slicerMap;
	XidTranslator xidTranslator;
	
	public FVClassifier(FVEventLoop loop, SocketChannel sock) {
		this.loop = loop;
		this.switchName = "unidentified:"+sock.toString();
		try {
			this.msgStream = new OFMessageAsyncStream(sock, new FVMessageFactory());
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, this, "IOException in constructor!");
			e.printStackTrace();
		}
		this.sock = sock;
		this.switchInfo = null;
		this.doneID = false;
		this.slicerMap = new HashMap<String,FVSlicer>();
		this.xidTranslator= new XidTranslator();
		
	}

	
	@Override
	public boolean needsConnect() {
		return false;			// never want connect events
	}

	@Override
	public boolean needsRead() {
		return true;			// always want read events
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


	public Map<String, FVSlicer> getSlicerMap() {
		return slicerMap;
	}


	public void setSlicerMap(Map<String, FVSlicer> slicerMap) {
		this.slicerMap = slicerMap;
	}


	public XidTranslator getXidTranslator() {
		return xidTranslator;
	}


	public void setXidTranslator(XidTranslator xidTranslator) {
		this.xidTranslator = xidTranslator;
	}


	/** 
	 * on init, send HELLO, delete all flow entries, and send features request
	 * @throws IOException
	 */
	
	public void init() throws IOException {
		msgStream.write(new OFHello());
		OFMatch match = new OFMatch();
		match.setWildcards(OFMatch.OFPFW_ALL);
		OFFlowMod fm = new OFFlowMod();
		fm.setMatch(match);
		fm.setCommand(OFFlowMod.OFPFC_DELETE);
		fm.setOutPort(OFPort.OFPP_NONE);
		fm.setBufferId(0xffffffff);		// buffer to NONE
		msgStream.write(fm);
		msgStream.write(new OFFeaturesRequest());
		msgStream.flush();
		int ops = SelectionKey.OP_READ;
		if (msgStream.needsFlush())
			ops |= SelectionKey.OP_WRITE;
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
		if ( Thread.currentThread().getId() != this.getThreadContext() )	{
			// this event was sent from a different thread context
			loop.queueEvent(e);  				// queue event
			return;								// and process later
		}
		if ( e instanceof FVIOEvent)
			handleIOEvent((FVIOEvent)e);		
		else
			throw new UnhandledEvent(e);
	}

	void handleIOEvent(FVIOEvent e) {
		int ops = e.getSelectionKey().readyOps();
	
		try {
			// read stuff, if need be
			if (( ops & SelectionKey.OP_READ) != 0) {
			    List<OFMessage> newMsgs = msgStream.read();
				if (newMsgs != null ) { 
				    for(OFMessage m : newMsgs) {
	                    FVLog.log(LogLevel.DEBUG, this, "read " + m);
				        if (switchInfo != null)
							classifyOFMessage(m);
						else 
							handleOFMessage_unidenitified(m);
					}
				}
			}
			// write stuff if need be
			if ((ops & SelectionKey.OP_WRITE) != 0 )
				msgStream.flush();
		} catch (IOException e1) {
			// connection to switch died; tear it down
			FVLog.log(LogLevel.INFO, this, "got IO exception; closing because : " + e1);
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
		try {
			this.sock.close();
			// shutdown each of the connections to the controllers
			Map<String,FVSlicer> tmpMap= slicerMap;
			slicerMap=null;		// to prevent tearDown(slice) corruption 
			for(FVSlicer fvSlicer : tmpMap.values()) 
					fvSlicer.tearDown();	
		} catch (IOException e) {
			// Silently ignore... already tearing down
		}
	}

	/** Main function
	 * Pass this message on to the appropriate Slicer
	 * as defined by XID, FlowSpace, config, etc.
	 * @param m
	 */
	private void classifyOFMessage(OFMessage msg) {
		FVLog.log(LogLevel.DEBUG, this, "received from switch: " + msg);
		((Classifiable)msg).classifyFromSwitch(this);  // msg specific handling
	}

	/** State machine for switches before we know
	 * which switch it is
	 * 
	 * Wait for FEATURES_REPLY; ignore everything else
	 * 
	 * @param m incoming message
	 */
	private void handleOFMessage_unidenitified(OFMessage m) {
		switch(m.getType()) {
			case HELLO:  // aleady sent our hello; just NOOP here
				if(m.getVersion() != OFMessage.OFP_VERSION) {
					FVLog.log(LogLevel.ALERT, this, "Mismatched version from switch " + 
							sock + " Got: "+ m.getVersion() + " Wanted: " + OFMessage.OFP_VERSION);
					this.tearDown();
	                   throw new RuntimeException("!!!");
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
				OFStatisticsRequest stats = new OFStatisticsRequest();
				stats.setStatisticType(OFStatisticsType.DESC);
				*/
				switchName = "dpid:" + String.format("%1$016x", switchInfo.getDatapathId());
				FVLog.log(LogLevel.INFO, this, 
				        	"identified switch as " + 
				        	switchName + " on " + 
				        	this.sock);
				this.connectToControllers();  // connect to controllers
				doneID = true;
				break;
			default : 
				// FIXME add logging
				FVLog.log(LogLevel.WARN, this, "Got unknown message type " + m + " to unidentified switch");
		}
	}

	/**
	 * Figure out which slice's have access to the switch and spawn an Slicer
	 * EventHandler for each of them.  Also, close the connection to any
	 * slice that is no longer listed
	 * 
	 * Assumes The switch is already been identified; 
	 * 
	 */
	private void connectToControllers() {
		Set<String> newSlices = FlowSpaceUtil.getSlicesByDPID(this.switchInfo.getDatapathId());
		// foreach slice, make sure it has access to this switch
		for(String sliceName : newSlices ) {
			if(! slicerMap.containsKey(sliceName)) {
				FVSlicer newSlicer = new FVSlicer(this.loop, this, sliceName);
				slicerMap.put(sliceName, newSlicer); // create new slicer in this same EventLoop
				newSlicer.init();	// and start it up
			}
		}
		// foreach slice with previous access, make sure it still has access
		for(String sliceName: slicerMap.keySet()) {
			if(!newSlices.contains(sliceName)) {
				// this slice no longer has access to this switch
				slicerMap.get(sliceName).tearDown();	
				slicerMap.remove(sliceName);
			}
		}
		
	}

	/**
	 * Called by FVSlicer to tell us to forget about them
	 * @param sliceName
	 */
	public void tearDown(String sliceName) {
		if (slicerMap!=null) {
			slicerMap.remove(sliceName);
			FVLog.log(LogLevel.DEBUG, this, "tore down slice " + sliceName + " on request");
		}
	}
}
