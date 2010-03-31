package org.flowvisor.classifier;

import org.flowvisor.events.*;
import org.flowvisor.exceptions.UnhandledEvent;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.*;
import org.openflow.protocol.factory.*;
import org.flowvisor.log.*;

import java.io.IOException;
import java.nio.channels.*;
/** 
 * Map OF messages from the switch to the appropriate slice
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
	
	public FVClassifier(FVEventLoop loop, SocketChannel sock) {
		this.loop = loop;
		this.switchName = "unidentified:"+sock.toString();
		this.doneID = false;
		try {
			this.msgStream = new OFMessageAsyncStream(sock, new BasicFactory());
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, this, "IOException in constructor!");
			e.printStackTrace();
		}
		this.sock = sock;
	}
	
	public void init() throws IOException {
		msgStream.write(new OFHello());
		msgStream.write(new OFFeaturesRequest());
		msgStream.flush();
		int ops = SelectionKey.OP_READ;
		if (msgStream.needsFlush())
			ops |= SelectionKey.OP_WRITE;
		loop.register(sock, ops, this);
	}
	
	@Override
	public String getName() {
		return "classifier:" + switchName;
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
			if (( ops & SelectionKey.OP_READ) != 0)
				for(OFMessage m : msgStream.read())
				{
					if (doneID)
						classifyOFMessage(m);
					else 
						handleOFMessage_unidenitified(m);
				}
			// write stuff if need be
			if ((ops & SelectionKey.OP_WRITE) != 0 )
				msgStream.flush();
		} catch (IOException e1) {
			// connection to switch died; tear it down
			this.tearDown();
		}
		// setup for next select
		if (msgStream.needsFlush())
			e.getSelectionKey().interestOps( SelectionKey.OP_READ + SelectionKey.OP_WRITE );
		else
			e.getSelectionKey().interestOps( SelectionKey.OP_READ);
	}

	/**
	 * Close all slice connections and cleanup
	 */
	private void tearDown() {
		// TODO close all Slice connections
		
	}

	/** Main function
	 * Pass this message on to the appropriate Slicer
	 * as defined by XID, FlowSpace, config, etc.
	 * @param m
	 */
	private void classifyOFMessage(OFMessage m) {
		// TODO Auto-generated method stub
		
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
			case HELLO:
				if(m.getVersion() != OFMessage.OFP_VERSION)
				{
					FVLog.log(LogLevel.ALERT, this, "Mismatched version from switch " + 
							sock + " Got: "+ m.getVersion() + " Wanted: " + OFMessage.OFP_VERSION);
					this.tearDown();
					return;
				}
			case ECHO_REQUEST:
				OFMessage echo_reply = new OFEchoReply();
				echo_reply.setXid(m.getXid());
				msgStream.write(echo_reply);
				break;
			case FEATURES_REPLY:
				identifySwitch(m);
				break;
			default : 
				// FIXME add logging
				FVLog.log(LogLevel.WARN, this, "Got unknown message type " + m + " to unidentified switch");
		}
	}

	private void identifySwitch(OFMessage m) {
		// TODO Auto-generated method stub
		
	}
}
