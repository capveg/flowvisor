/**
 *
 */
package org.flowvisor.log;

import org.flowvisor.events.FVEventHandler;

/**
 * Static method
 * 
 * @author capveg
 * 
 */
public class FVLog {
	static boolean needsInit = true;
	static FVLogInterface logger = new StderrLogger();
	static LogLevel threshold = LogLevel.DEBUG; // min level for logging

	/**
	 * Wrapper around the default logger
	 * 
	 * @param level
	 *            Log priority
	 * @param source
	 *            who sent the log message (can be null)
	 * @param msg
	 *            Log message
	 */
	public static synchronized void log(LogLevel level, FVEventHandler source,
			String msg) {
		if (needsInit)
			doInit();
		if (level.ordinal() <= threshold.ordinal())
			logger.log(level, System.currentTimeMillis(), source, msg);
	}

	private static void doInit() {
		needsInit = false;
		logger.init();
	}

	/**
	 * Change the default logger
	 * 
	 * @param logger
	 *            New logger
	 */
	public static synchronized void setDefaultLogger(FVLogInterface logger) {
		FVLog.logger = logger;
		needsInit = true;
	}

	/**
	 * Get the logging threshold
	 * 
	 */
	public static LogLevel getThreshold() {
		return FVLog.threshold;
	}

	/**
	 * Set the logging threshold All logs equal to or greater than this level
	 * are logged
	 */
	public void setThreshold(LogLevel l) {
		FVLog.threshold = l;
	}
}
