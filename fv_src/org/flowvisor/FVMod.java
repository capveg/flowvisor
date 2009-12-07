package org.flowvisor;

import java.nio.channels.*;
import org.flowvisor.events.*;
import org.flowvisor.exceptions.*;

/****
 * FVMod is a abstract class for handling openflow events.
 * 
 * @author capveg
 *
 */
abstract public class FVMod 
{
	String name;
	FVPollLoop pollLoop;
	SocketChannel sock;
	public FVMod(String name, FVPollLoop pollLoop)
	{
		this.name = name;
		this.pollLoop = pollLoop;
	}
	
	public void setSocketChannel(SocketChannel sock)
	{
		this.sock = sock;
	}
	
    public void handleEvent(FVEvent event) throws UnhandledEvent
    {
    	switch(event.getType())
    	{
    		case FVEvent.FVET_IO:
    			handleIOEvent((FVIOEvent) event);
    			break;
    		case FVEvent.FVET_OF_MSG:
    			handleOFEvent((FVOFEvent) event);
    			break;
    		case FVEvent.FVET_TIMER:
    			handleTimerEvent((FVTimerEvent) event);
    			break;
    		default:
    			throw new UnhandledEvent("don't know how to deal with event" + event);    			
    	}
    }
    public void handleIOEvent(FVIOEvent event) throws UnhandledEvent
    { 
    	throw new UnhandledEvent("didn't implement IO event handler"); 
    }
    public void handleOFEvent(FVOFEvent event) throws UnhandledEvent
    {
    	throw new UnhandledEvent("didn't implement OF event handler"); 	
    }
    public void handleTimerEvent(FVTimerEvent event) throws UnhandledEvent
    {
    	throw new UnhandledEvent("didn't implement Timer event handler"); 	
    }
    
    public String getName() { return this.name; }

    public void cleanup() 
    {
    	// NOOP by default
    }
}
