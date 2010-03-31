/**
 * 
 */
package org.flowvisor.log;

import org.flowvisor.events.FVEventHandler;

/**
 * @author capveg
 *
 */
public class StderrLogger implements FVLogInterface {

	/* (non-Javadoc)
	 * @see org.flowvisor.log.FVLogInterface#log(org.flowvisor.log.FVLogLevel, org.flowvisor.events.FVEventHandler, java.lang.String)
	 */
	@Override
	public void log(LogLevel level, FVEventHandler source, String msg) {
		if (source != null)
			System.err.println(level.toString() + ":" + source.getName() + ":: " + msg);
		else
			System.err.println(level.toString() + ":none:: " + msg);
	}

}
