package org.flowvisor.ofswitch;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.sql.Time;

import org.flowvisor.*;
import org.flowvisor.events.*;

public class OFSwitchAcceptor extends FVMod
{
    String name;
    FVPollLoop pollLoop;
    int backlog;
    int listenPort;
    ServerSocketChannel ssc;
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

        // register this module with the polling loop
        pollLoop.register(ssc, SelectionKey.OP_ACCEPT, this);
    }

    public void handleIOEvent(FVIOEvent event)
    {
 		SocketChannel sock = null;
 	   
    	try 
    	{
    		sock = ssc.accept();
    		System.err.println("GOt a new swtich connection: " + sock.socket());
    		pollLoop.addTimer( new FVTimerEvent(this, 1000, sock));
    	}
    	catch (IOException e)		// ignore IOExceptions -- is this the right thing to do?  	 
    	{
    		System.err.println("Got IOException for " + sock!= null? sock : "unknown socket");
    		System.err.println(e);
    	}
    }
    
    public void handleTimerEvent(FVTimerEvent e)
    {
    	SocketChannel sock = (SocketChannel) e.getArg();   	
    	try 
    	{
    		System.err.println("Got timer event at " + new Time(System.currentTimeMillis()) + " for " + sock);
    		sock.write(ByteBuffer.allocate(256).put("Hello socket!\n".getBytes()));
	    	pollLoop.addTimer( new FVTimerEvent(this, 1000, sock));   	
    	}
    	catch(IOException ex)
    	{
    		System.err.println("IOException for "+sock);
    		System.err.println(ex);
    	}
    }   
}
