/**
 * 
 */
package org.flowvisor.events;

import org.flowvisor.exceptions.*;

/**
 * Basic processing module for message passing in FV
 * @author capveg
 *
 */
public interface FVEventHandler {
	
	/**
	 * Pass a message to this Handler.
	 * If the dst is in the same thread context as the caller,
	 * 	the message is processed immediately (blocking)
	 * Else, the message is queued and processed later (non-blocking)
	 * 	If sender is null, the message is always queued
	 * @param e A FVEvent 
	 */
	public void handleEvent(FVEvent e) throws UnhandledEvent;
	
	/** 
	 * Returns an integer that identifies the threading context that this
	 * handler runs in
	 * @return A long that identifies this thread
	 */
	public long getThreadContext();
	
	/** 
	 * Return a human readable string (no spaces) that describes this
	 * handler
	 * @return String
	 */
	 public String getName(); 
	 
	 /**
	  * Tell the Handler to stop what it's doing and clean up all of its state
	  */
	 public void tearDown();
	
}
