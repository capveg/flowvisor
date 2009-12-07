package org.flowvisor.events;

import org.flowvisor.FVMod;

public class FVTimerEvent extends FVEvent implements Comparable<FVTimerEvent> {
		long expire_time;
		static int ID = 0;
		Object arg;
	    int id;
		public FVTimerEvent(FVMod src, long expire, Object arg)
	    {
	        super(src,FVET_TIMER,0);
	        this.expire_time = System.currentTimeMillis() + expire;
	        this.arg = arg;
	        this.id = ID++;
	    }
	    
	    public int compareTo(FVTimerEvent e) 
	    {
	    	return Long.valueOf(this.expire_time - e.expire_time).intValue();
	    }
	    
	    public Object getArg()  { return this.arg; }
	    public int    getID()   { return this.id;  }
	    public long getExpireTime() { return this.expire_time;}
}
