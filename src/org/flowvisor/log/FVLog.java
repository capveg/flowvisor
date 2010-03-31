/**
 * 
 */
package org.flowvisor.log;

import org.flowvisor.events.FVEventHandler;

/** Static method 
 * @author capveg
 *
 */
public class FVLog {

	static FVLogInterface logger = new StderrLogger();
	
	/** Wrapper around the default logger 
	 * 
	 * @param level Log priority
	 * @param source who sent the log message (can be null)
	 * @param msg Log message
	 */
	public static void log(LogLevel level, FVEventHandler source, String msg) {
		logger.log(level, source, msg);
	}
	
	/** Change the default logger
	 *
	 * @param logger New logger
	 */
	public static void setDefaultLogger(FVLogInterface logger) {
		FVLog.logger = logger;
	}

}
