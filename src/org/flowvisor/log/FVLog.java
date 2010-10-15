/**
 *
 */
package org.flowvisor.log;

import org.flowvisor.FlowVisor;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.FVEventHandler;

/**
 * Static method
 * 
 * @author capveg
 * 
 */
public class FVLog {
	static boolean needsInit = true;
	static FVLogInterface logger = new SyslogLogger();
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
		boolean needConfigFlush = false;
		try {
			threshold = LogLevel.valueOf(FVConfig
					.getString(FVConfig.LOG_THRESH));
		} catch (ConfigError e) {
			System.err.println("--- '" + FVConfig.LOG_THRESH
					+ "' not set in config; defaulting to loglevel 'DEBUG'");
			try {
				FVConfig.setString(FVConfig.LOG_THRESH, LogLevel.DEBUG
						.toString());
				needConfigFlush = true;

			} catch (ConfigError e1) {
				throw new RuntimeException(e1);
			}
			threshold = LogLevel.DEBUG;
		}
		try {
			logger.init();
		} catch (UnsatisfiedLinkError e) {
			System.err
					.println("Unable to load default logger; failing over to stderr: "
							+ e);
			logger = new StderrLogger();
			logger.init();
		}
		System.err.println("--- Setting logging level to " + threshold);
		if (needConfigFlush)
			FlowVisor.getInstance().checkPointConfig();
		for (LogLevel level : LogLevel.class.getEnumConstants()) {
			if (level != LogLevel.FATAL) // fatal gets broadcasted to console
				FVLog.log(level, null, "log level enabled: " + level);
		}
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
