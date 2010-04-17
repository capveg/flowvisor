package org.flowvisor.ofswitch;

import java.net.*;
import java.io.*;
import java.nio.channels.*;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.events.*;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.log.*;

import java.util.*;

public class OFSwitchAcceptor implements FVEventHandler
{
    String name;
    FVEventLoop pollLoop;
    
    int backlog;
    int listenPort;
    ServerSocketChannel ssc;
    List<FVClassifier> switches;
    public OFSwitchAcceptor(String name, FVEventLoop pollLoop, int port, int backlog) throws IOException
    {
        this.listenPort = port;
        this.backlog = backlog;
        this.pollLoop = pollLoop;
        
        ssc = ServerSocketChannel.open();
        ssc.socket().setReuseAddress(true);
        ssc.socket().bind(new InetSocketAddress(port), backlog);
        ssc.configureBlocking(false);
    	FVLog.log(LogLevel.INFO, this, "Listenning on port " + port); 

        switches = new ArrayList<FVClassifier>();
        // register this module with the polling loop
        pollLoop.register(ssc, SelectionKey.OP_ACCEPT, this);
    }

    
    
    @Override
	public boolean needsConnect() {
		return false;
	}

	@Override
	public boolean needsRead() {
		return false;
	}

	@Override
	public boolean needsWrite() {
		return false;
	}

	@Override
	public boolean needsAccept() {
		return true;
	}



	@Override
    public long getThreadContext() {
    	return pollLoop.getThreadContext();
    }
 
    @Override
	public void tearDown() {
		
    	try {
			ssc.close();
		} catch (IOException e) {
			// ignore if shutting down throws an error... we're already shutting down
		}		
	}

	@Override
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		if ( Thread.currentThread().getId() != this.getThreadContext() )	{
			// this event was sent from a different thread context
			pollLoop.queueEvent(e);  				// queue event
			return;								// and process later
		}
		if ( e instanceof FVIOEvent)
			handleIOEvent((FVIOEvent)e);
		else
			throw new UnhandledEvent(e);
	}
    
    void handleIOEvent(FVIOEvent event)
    {
 		SocketChannel sock = null;
 	   
    	try 
    	{
    		sock = ssc.accept();
    		if (sock == null ) {
    			FVLog.log(LogLevel.CRIT, null, "ssc.accept() returned null !?! FIXME!");
    			return;
    		}
    		FVLog.log(LogLevel.INFO, this, "got new connection: " + sock);
    		FVClassifier fvc = new FVClassifier(pollLoop, sock); 
      		fvc.init();
    		switches.add(fvc);
    	}
    	catch (IOException e)		// ignore IOExceptions -- is this the right thing to do?  	 
    	{
    		System.err.println("Got IOException for " + sock!= null? sock : "unknown socket");
    		System.err.println(e);
    	}
    }

	@Override
	public String getName() {
		return "OFSwitchAcceptor";
	}   
}
