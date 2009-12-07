package org.flowvisor;

import java.nio.channels.*;
import org.flowvisor.events.*;
import org.flowvisor.exceptions.*;
import org.flowvisor.fvtimer.FVTimer;
import java.io.*;
import java.util.*;

public class FVPollLoop extends FVMod
{

    Selector selector;
    FVTimer fvtimer;
    boolean dontStop;
    public FVPollLoop() throws IOException
    {
    		super("pollLoop",null);		// kinda silly, but simplifies things
    		selector = Selector.open();
    		fvtimer  = new FVTimer();
    		dontStop = true;
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

    /****
     * Pass a timer event on to the Timer class
     * @param e
     */
    public void addTimer(FVTimerEvent e)
    {
    	fvtimer.addTimer(e);
    }

    /****
     * Main top-level IO loop
     * 	this dispatches all IO events and timer events together
     * I believe this is fairly efficient
     */
    public void doPollLoop() throws IOException,UnhandledEvent
    {
    	while(dontStop)
    	{
    		long nextTimerEvent;
        	int nEvents;
    		
        	nextTimerEvent = this.fvtimer.processEvent();  // this fires off a timer event if it's ready
    		nEvents = selector.select(nextTimerEvent);
    		if(nEvents > 0)
    		{
    			for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) 
    			{
    				SelectionKey sk = i.next();
    				i.remove();  // copied from example, not clear why this is here
    			    FVMod mod = (FVMod)sk.attachment();
    			    mod.handleEvent(new FVIOEvent( this, sk));
    			}   		
    		}
    	}
    }
}
