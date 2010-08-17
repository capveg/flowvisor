/**
 * 
 */
package org.flowvisor.log;

import org.flowvisor.events.FVEventHandler;
import org.flowvisor.log.Syslog.Facility;

/**
 * @author capveg
 * 
 */
public class SyslogLogger implements FVLogInterface {

	Syslog syslog;
	static public Facility DEFAULT_LOGGING_FACILITY = Facility.LOG_LOCAL7;
	static public String DEFAULT_LOGGING_IDENT = "flowvisor";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.log.FVLogInterface#init()
	 */
	@Override
	public void init() {
		this.syslog = new Syslog(DEFAULT_LOGGING_FACILITY,
				DEFAULT_LOGGING_IDENT);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.log.FVLogInterface#log(org.flowvisor.log.LogLevel,
	 * long, org.flowvisor.events.FVEventHandler, java.lang.String)
	 */
	@Override
	public void log(LogLevel level, long time, FVEventHandler source, String msg) {
		syslog.log(level.ordinal(), msg);
	}

}
