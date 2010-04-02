/**
 * 
 */
package org.flowvisor.config;

import java.util.HashSet;
import java.util.Set;

import org.flowvisor.events.*;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;


/**
 * An abstract class entry in the Config
 * @author capveg
 *
 */
public abstract class ConfigEntry {
	String name;
	ConfigType type;
	Set<FVEventHandler> watchList;		// never gets saved across sessions
	boolean persistent;   				// does this config entry get saved across FV sessions?
	
	public ConfigEntry(String name, ConfigType type) {
		this.name = name;
		this.type = type;
		this.persistent = true;
		this.watchList = new HashSet<FVEventHandler>();
	}
	
	public String getName() {
		return this.name;
	}
	public ConfigType getType() {
		return this.type;
	}
	
	/**
	 * Add this {@link FVEventHandler} to the list of things that get updated if this
	 * config entry changes
	 * @param eh
	 */
	public void watch(FVEventHandler eh) {
		watchList.add(eh);
	}
	
	/**
	 * Remove this {@link FVEventHandler} from the list of things that get updated on config change
	 * @param eh
	 */
	public void unwatch(FVEventHandler eh) {
		watchList.remove(eh);
	}
	
	/**
	 * Convert from string to the given value
	 * @param val
	 */
	public void setValue(String val) {
		// FIXME: find the java way of doing this
		throw new RuntimeException("need to override this... ");
	}
	
	/** 
	 * Convert the node's value to a string
	 * @return
	 */
	public String getValue() {
		// FIXME: find the java way of doing this
		throw new RuntimeException("need to override this... ");
	}
	
	void sendUpdates() {
		for(FVEventHandler eh : watchList) {
			try {
				eh.handleEvent(new ConfigUpdateEvent(eh, this.name));
			} catch (UnhandledEvent e) {
				FVLog.log(LogLevel.CRIT, eh, "Doesn't handle ConfigUpdateEvent but asked for them !?");
			}
		}
	}
	
	/** 
	 * Does this config entry get saved across FV sessions?
	 * Default is yes.
	 * @return
	 */
	public boolean getPersistent() {
		return this.persistent;
	}
	
	/** 
	 * Set whether this config entry gets saved across FV sessions
	 * @param val
	 */
	public void setPersistent(boolean val) {
		this.persistent = val;
	}
}
