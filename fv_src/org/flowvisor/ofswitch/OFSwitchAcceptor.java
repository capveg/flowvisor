package org.flowvisor.ofswitch;

import java.net.*;
import java.io.*;
import java.nio.channels.*;

import org.flowvisor.*;
import org.flowvisor.events.*;
import java.util.*;

public class OFSwitchAcceptor extends FVMod
{
    String name;
    FVPollLoop pollLoop;
    int backlog;
    int listenPort;
    ServerSocketChannel ssc;
    List<OFSwitch> switches;
    public OFSwitchAcceptor(String name, FVPollLoop pollLoop, int port, int backlog) throws IOException
    {
        super(name,pollLoop);
        if (pollLoop == null)
        	throw new NullPointerException();
        this.listenPort = port;
        this.backlog = backlog;
        this.pollLoop = pollLoop;
        
        ssc = ServerSocketChannel.open();
        ssc.socket().setReuseAddress(true);
        ssc.socket().bind(new InetSocketAddress(port), backlog);
        ssc.configureBlocking(false);

        switches = new ArrayList<OFSwitch>();
        // register this module with the polling loop
        pollLoop.register(ssc, SelectionKey.OP_ACCEPT, this);
    }

    public void handleIOEvent(FVIOEvent event)
    {
 		SocketChannel sock = null;
 	   
    	try 
    	{
    		sock = ssc.accept();
    		switches.add(new OFSwitch(sock,pollLoop));
    	}
    	catch (IOException e)		// ignore IOExceptions -- is this the right thing to do?  	 
    	{
    		System.err.println("Got IOException for " + sock!= null? sock : "unknown socket");
    		System.err.println(e);
    	}
    }   
}
