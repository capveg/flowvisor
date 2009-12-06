package org.flowvisor;

import java.nio.channels.*;
import org.flowvisor.events.*;
import java.io.*;

public class FVPollLoop 
{

    Selector selector;

    public FVPollLoop() throws IOException
    {
    		selector = Selector.open();
   
    }


    public void register(SelectableChannel ch, int ops, FVMod mod)
    {
    	try 
    	{
    		ch.register(selector, ops, mod);
    	}
    	catch (ClosedChannelException e)
    	{
    		// FIXME : log an error?
    	}
    }

    public void addTimer(FVMod mod, FVTimerEvent t)
    {
    
    }

    public void doPollLoop()
    {
        // get time until next event
        // do select
        // if not timeout, dispatch events
    }
}
