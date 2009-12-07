package org.flowvisor.fvtimer;

import org.flowvisor.events.*;
import org.flowvisor.exceptions.*;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.sql.Time;

/***
 * A priority queue of timer events
 * 	does NOT actually call events directly.
 * External callers (e.g., pollLoop) do
 * 			addTimer()
 * and repeated call processTimer()
 * 
 * NOT threadsafe!
 * 
 * @author capveg
 *
 */
public class FVTimer {
	public static final long MAX_TIMEOUT = 1000;
	
	PriorityQueue<FVTimerEvent> pq;
	public FVTimer()
	{
		pq = new PriorityQueue<FVTimerEvent>();
		if(pq == null)
			throw new NullPointerException();
	}
	 
	public void addTimer(FVTimerEvent e)
	{
		System.err.println("Scheduleing event at t=" + new Time(System.currentTimeMillis()) +
					" to happen at " + new Time(e.getExpireTime()));
		pq.add(e);
	}
	/***
	 * Compare the current wall clock time to the next event in the queue.
	 * If there is nothing in the queue, return MAX_TIMEOUT
	 * If the time for this event has passed, process it (only one event per call) and return 0
	 * Else, return the time in milliseconds until the next event
	 */
	public long processEvent() throws UnhandledEvent
	{
		long now = System.currentTimeMillis();
		FVTimerEvent e = this.pq.peek();
		
		if(e == null)
			return MAX_TIMEOUT;
		long expire = e.getExpireTime();
		if(now >= expire)
		{
			pq.remove();
			e.getSrc().handleEvent(e);
			return 0;
		}
		else
			return expire - now;
	}
	/****
	 * Cancels a timer that has previously been added via addTimer()
	 * @param id the id of the timer as returned by FVTimerEvent.getID()
	 * @return true if found and removed, else false
	 */
	boolean removeTimer(int id)
	{
		FVTimerEvent e;
		Iterator<FVTimerEvent> it = pq.iterator();
		for(e=it.next(); it.hasNext(); e = it.next())
		{
			if (e.getID() == id)
			{
				pq.remove(e);
				return true;
			}
		}
		return false;	
	}
}
