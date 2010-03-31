/**
 * 
 */
package org.flowvisor.log;

import org.flowvisor.events.FVEventHandler;

/** Generic interface for logging in FV
 * 
 * @author capveg
 *
 */
public interface FVLogInterface {

	/**
	 * Log a message
	 * 
	 * @param level Priority of message
	 * @param source Source of message; might be null
	 * @param msg  Actual message
	 */
	public void log(LogLevel level, FVEventHandler source, String msg);
}
