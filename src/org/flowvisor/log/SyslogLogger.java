/**
 * 
 */
package org.flowvisor.log;

import org.flowvisor.config.FVConfig;
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

	Facility facility;
	String ident;

	public SyslogLogger() {
		this.facility = DEFAULT_LOGGING_FACILITY;
		this.ident = DEFAULT_LOGGING_IDENT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.log.FVLogInterface#init()
	 */
	@Override
	public void init() {

		try {
			String fac = FVConfig.getString(FVConfig.LOG_FACILITY);
			this.facility = Syslog.Facility.valueOf(fac);
			if (this.facility == null) {
				this.facility = DEFAULT_LOGGING_FACILITY;
				System.err
						.println("Invalid logging facitily: failing back to default: '"
								+ fac + "'");
			}
		} catch (Exception e) {

		}
		try {
			this.ident = FVConfig.getString(FVConfig.LOG_IDENT);
		} catch (Exception e) {

		}
		this.syslog = new Syslog(facility, ident);
		this.syslog.log(Syslog.Priority.LOG_INFO, "started flowvisor syslog");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.flowvisor.log.FVLogInterface#log(org.flowvisor.log.LogLevel,
	 * long, org.flowvisor.events.FVEventHandler, java.lang.String)
	 */
	@Override
	public void log(LogLevel level, long time, FVEventHandler source, String msg) {
		if (level != LogLevel.MOBUG)
			syslog.log(level.getPriority(), level.toString() + ": " + msg);
	}

}
