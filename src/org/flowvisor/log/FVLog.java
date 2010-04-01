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
	static LogLevel threshold = LogLevel.INFO;	// min level for logging
	
	/** Wrapper around the default logger 
	 * 
	 * @param level Log priority
	 * @param source who sent the log message (can be null)
	 * @param msg Log message
	 */
	public static void log(LogLevel level, FVEventHandler source, String msg) {
		if(level.ordinal() <= threshold.ordinal())
			logger.log(level, source, msg);
	}
	
	/** Change the default logger
	 *
	 * @param logger New logger
	 */
	public static void setDefaultLogger(FVLogInterface logger) {
		FVLog.logger = logger;
	}
	
	/** Get the logging threshold
	 * 
	 */
	public static LogLevel getThreshold() {
		return FVLog.threshold;
	}

	/** Set the logging threshold
	 * All logs equal to or greater than this level are logged
	 */
	public void setThreshold(LogLevel l) { 
		FVLog.threshold = l;
	}
}

