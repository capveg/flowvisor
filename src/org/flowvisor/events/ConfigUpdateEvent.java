/**
 *
 */
package org.flowvisor.events;

import org.flowvisor.events.FVEvent;

/**
 * Let an event handler know that a config element that it was watching needs
 * updating
 * 
 * @author capveg
 * 
 */
public class ConfigUpdateEvent extends FVEvent {
	String config;

	/**
	 * @param dst
	 *            Destination event handler
	 * @param config
	 *            The config element that needs updating
	 */
	public ConfigUpdateEvent(FVEventHandler dst, String config) {
		super(null, dst);
		this.config = config;
	}

	/**
	 * Get the name of the config element that needs updating
	 */
	public String getConfig() {
		return this.config;
	}

}
