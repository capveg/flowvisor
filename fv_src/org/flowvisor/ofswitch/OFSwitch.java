package org.flowvisor.ofswitch;

import java.nio.channels.*;
import java.util.*;
import java.net.*;
import java.io.*;
import org.flowvisor.*;
import org.flowvisor.events.FVIOEvent;
import org.openflow.*;

/*****
 * This class handles events coming from and going to an OpenFlow switch.
 * 
 * @author capveg
 *
 */
public class OFSwitch extends FVMod
{
	static int unknown_count = 1;

	boolean identified;
	long dpid;
	SocketChannel sock;
	LinkedList<OFMessage> outgoing;
	
	public OFSwitch(SocketChannel sock, FVPollLoop pollLoop)
	{
		super("unknown_ofswitch#"+ unknown_count++, pollLoop);
		this.sock = sock;
		try
		{
			sock.configureBlocking(false);
			sock.socket().setTcpNoDelay(true);
		}
		catch (SocketException se)
		{
			System.err.println(se);
		}
		catch (IOException ie)
		{
			System.err.println(ie);
		}

		this.outgoing = new LinkedList<OFMessage>();
		
		this.send(new OFHello());			// this implicitly registers with pollLoop
		this.send(new OFFeaturesRequest()); // FIXME: should negotiate first
		this.send(makeFlowModFlush());
		
		this.identified=false;
	}

	public void send(OFMessage msg)
	{
		boolean isEmpty = this.outgoing.isEmpty();
		this.outgoing.add(msg);
		
		if(isEmpty)  // register for writing events if we haven't already
			pollLoop.register(sock, SelectionKey.OP_READ|SelectionKey.OP_WRITE, this);
	}
	
	public OFMessage makeFlowModFlush()
	{
		OFFlowMod flush = new OFFlowMod();
		flush.setXID(0xcafedeadbeefbeefl);
		return flush;
	}
	
	public void handleIOEvent(FVIOEvent e)
	{
		int ready = e.getCode();
		if ( (ready & SelectionKey.OP_READ) != 0)
		{
			if(this.identified)
				handleReadEvent(e);
			else 
				handleUnidentifiedReadEvent(e);
		}
		if ( (ready & SelectionKey.OP_WRITE) != 0)
			handleWriteEvent(e);
	}
	
	public void handleWriteEvent(FVIOEvent e)
	{
		if (! outgoing.isEmpty())
		{
			try
			{
				OFMessage msg = outgoing.peek();
				int count = msg.writeToSocketChannel(this.sock);
				if (count != msg.getLength())
					outgoing.poll();	// successfully sent
				if ( outgoing.isEmpty())
					this.pollLoop.register(this.sock, SelectionKey.OP_READ, this);
			}
			catch (IOException ie)
			{
				System.err.println(ie);
				cleanup();
			}
		}
	}
	
	public void cleanup()
	{
		super.cleanup();
		try 
		{
			this.sock.close();
		}
		catch (IOException ie)
		{
				// do nothing; we're dying anyway
		}
	}
	public void handleUnidentifiedReadEvent(FVIOEvent e)
	{
		// FIXME: put classification logic here 
	}
	public void handleReadEvent(FVIOEvent e)
	{
		// FIXME: put classification logic here 
	}
	
}
