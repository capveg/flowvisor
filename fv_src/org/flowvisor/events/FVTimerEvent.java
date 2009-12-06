package org.flowvisor.events;

import org.flowvisor.FVMod;

public class FVTimerEvent extends FVEvent implements Comparable<FVTimerEvent> {
		long expire_time;
		static int ID = 0;
		FVMod mod;
		Object arg;
	    int id;
		public FVTimerEvent(FVMod src, FVMod dst, long expire, Object arg)
	    {
	        super(src,FVET_TIMER,0);
	        this.mod = dst;
	        this.expire_time = System.currentTimeMillis() + expire;
	        this.arg = arg;
	        this.id = ID++;
	    }
	    
	    public int compareTo(FVTimerEvent e) 
	    {
	    	return Long.valueOf(this.expire_time - e.expire_time).intValue();
	    }
	    
	    public FVMod getFVMod() { return this.mod; }
	    public Object getArg()  { return this.arg; }
	    public int    getID()   { return this.id;  }
	    public long getExpireTime() { return this.expire_time;}
}
