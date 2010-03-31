/**
 * 
 */
package org.flowvisor.events;

import org.flowvisor.exceptions.*;

/**
 * Basic implementation of an event handler
 *  Both for reference and used in derived classes
 *   
 * @author capveg
 *
 */
public class ExampleHandler implements FVEventHandler {
	FVEventLoop loop;
	
	/** 
	 * Construct a basic event handler
	 * @param loop the event loop this handler will reside in 
	 */
	public ExampleHandler(FVEventLoop loop) {
		this.loop = loop;
	}
	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#getThreadContext()
	 */
	@Override
	public long getThreadContext() {
		return loop.getThreadContext();
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#handleEvent(org.flowvisor.events.FVEvent)
	 */
	@Override
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		if ( Thread.currentThread().getId() != this.getThreadContext() )	{
			// this event was sent from a different thread context
			loop.queueEvent(e);  				// queue event
			return;								// and process later
		}
		// do something with the event here
		// ....
		
		// throw unhandled event for everthing else
		throw new UnhandledEvent(e);
	}
	
	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#getName()
	 */
	public String getName() {
		return "ExampleHandler";
	}
}
