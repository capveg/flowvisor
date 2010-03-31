/**
 * 
 */
package org.flowvisor.events;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.fvtimer.FVTimer;
import org.flowvisor.events.FVEvent;
/**
 * @author capveg
 *
 */
public class FVEventLoop {
	int thread_id;
	boolean shouldStop;
    Selector selector;
    FVTimer fvtimer;
    List<FVEvent> eventQueue;

	public FVEventLoop() throws IOException {
		this.shouldStop = false;
		this.selector = Selector.open();
		this.fvtimer  = new FVTimer();
		this.eventQueue = new LinkedList<FVEvent>();
	}
	
    public void register(SelectableChannel ch, int ops, FVEventHandler handler)
    {
    	try 
    	{
    		ch.register(selector, ops, handler);
    	}
    	catch (ClosedChannelException e)
    	{
    		// FIXME : log an error?
    	}
    }
    
    public void queueEvent(FVEvent e) {
    	synchronized (eventQueue) {
        	eventQueue.add(e);			
		}
    	selector.wakeup();	// tell the selector to come out of sleep: awesome call!
    }

    public long getThreadContext() {
    	return Thread.currentThread().getId();
    }
    
    /****
     * Pass a timer event on to the Timer class
     * @param e
     */
    public void addTimer(FVTimerEvent e)
    {
    	fvtimer.addTimer(e);
    }
    
    /** 
     * Tell this EventLoop to stop
     */
    public void shouldStop() {
    	shouldStop = true;
    }

    /****
     * Main top-level IO loop
     * 	this dispatches all IO events and timer events together
     * I believe this is fairly efficient for processing IO events
     * and events queued should cause the select to wake up 
     * 
     */
    public void doEventLoop() throws IOException,UnhandledEvent
    {
    	while (! shouldStop)
    	{
    		long nextTimerEvent;
        	int nEvents;
        	List<FVEvent> tmpQueue = null;
        	
        	// copy queued events out of the way and clear queue
        	synchronized (eventQueue) {  
            	if (!eventQueue.isEmpty() ) {
            		tmpQueue = eventQueue;
            		eventQueue = new LinkedList<FVEvent>();
            	}
        	}
    	
        	// process all queued events, if any
        	if (tmpQueue != null)
        		for (FVEvent e : tmpQueue)
        			e.getDst().handleEvent(e);
        	
        	// calc time until next timer event
        	nextTimerEvent = this.fvtimer.processEvent();  // this fires off a timer event if it's ready
    		
        	// wait until next IO event or timer event
        	nEvents = selector.select(nextTimerEvent);
    		if(nEvents > 0)
    		{
    			for (SelectionKey sk : selector.selectedKeys()) 
    			{
    				if(sk.isValid()) {  // skip any keys that have been canceled
    					FVEventHandler handler = (FVEventHandler)sk.attachment();   			    
    					handler.handleEvent(new FVIOEvent(sk, null, handler));
    				}
    			}
    			selector.selectedKeys().clear();	// mark all keys as processed
    		}
    	}
    }
}
