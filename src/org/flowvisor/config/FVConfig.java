/**
 * 
 */
package org.flowvisor.config;

import java.util.*;

/**
 * Central collection of all configuration and policy information, e.g., 
 * slice permissions, what port to run on, etc.
 * 
 * Uses get/set on a hierarchy of nodes like sysctl,snmp, etc.
 * getInt("flowvisor.list_port") --> 6633
 * setString("slice.alice.controller_hostname","alice-controller.controllers.org")
 * 
 * All of the set* operations will dynamically create the entry if it does not exist. 
 * 
 * @author capveg
 *
 */
public class FVConfig {
	static ConfDirEntry root = new ConfDirEntry(".");  // base of all config info
	
	/** 
	 * Return the config entry specific in name
	 * @param name
	 * @return null if not found
	 */
	static private ConfigEntry lookup(String name) {
		List<String> parts = Arrays.asList(name.split("."));
		ConfigEntry ret = null;
		ConfDirEntry base = FVConfig.root;
		for(String part: parts) {
			if (base == null)
				break;
			ret = base.lookup(part);
			if(ret == null)
				break;
			if (ret.getType() == ConfigType.DIR)
				base = (ConfDirEntry) ret;
			else
				base = null;	
		}
		return ret;
	}
	
	static private ConfigEntry create(String name, ConfigType type) throws ConfigError {
		String[] parts = name.split(".");
		int i;
		ConfDirEntry base = FVConfig.root;
		
		// step through tree; creating as we go
		for ( i=0 ; i < (parts.length -1) ; i++ ) {
			ConfigEntry tmp = base.lookup(parts[i]);
			
			if (tmp == null ) {
				tmp = new ConfDirEntry(parts[i]);
				base.add(tmp);
			} else if (tmp.getType() != ConfigType.DIR){
				throw new ConfigCantCreateError("tried to create dir \"" + 
						name + "\"" + 
						" but element " + i  +
						" \"" + parts[i] + " is a " + 
						tmp.getType() + " not a directory");
			}
			base = (ConfDirEntry) tmp;			 				
		}
		// magic up a new instance of 
		Class<? extends ConfigEntry> c = type.toClass();
		ConfigEntry entry;
		try {
			entry = c.getConstructor(new Class[]{String.class}).newInstance();
		} catch (Exception e) {
			throw new ConfigCantCreateError(e.toString());
		}
		// add it to the (potentially newly created) base
		base.add(entry);
		return entry;
	}
	
	/** 
	 * Sets an integer in the config
	 * Will dynamically create the path if it does not exist
	 * @param node e.g., "path.to.configname"
	 * @param val any integer
	 * @throws ConfigError If trying to create the path conflicted with existing config
	 */
	
	static public void setInt(String node, int val) throws ConfigError {
		ConfigEntry entry = FVConfig.lookup(node);
		if (entry == null ) 
			entry = create(node, ConfigType.INT);
		else 
			throw new ConfigWrongTypeError("tried to set an " + entry.getType() + " to int");
		ConfIntEntry ei = (ConfIntEntry)entry;
		ei.setInt(val);
	}
	
	/** 
	 * Return the integer associated with this node
	 * 
	 * @param node Full path to node
	 * @return integer
	 * @throws ConfigError If entry not found or if not an int
	 */
	static public int getInt(String node) throws ConfigError {
		ConfigEntry entry = FVConfig.lookup(node);
		if (entry == null)
			throw new ConfigNotFoundError("node " + node + " does not exist");
		if (entry.getType() != ConfigType.INT)
			throw new ConfigWrongTypeError("tried to get an int but got a " +  entry.getType());
		return ((ConfIntEntry)entry).getInt();
	}
	
	/**
	 * Returns a list of nodes at this subdirectory 
	 * @param base
	 * @return List of nodes
	 * @throws ConfigError
	 */
	static public List<String> list(String base) throws ConfigError{
		ConfigEntry e = lookup(base);
		if(e == null)
			throw new ConfigNotFoundError("base not found: " + base);
		if(e.getType() != ConfigType.DIR)
			throw new ConfigWrongTypeError("node " + base + " is a "  +
					e.getType() + ", not a DIR");
		return ((ConfDirEntry)e).list();
		
	}
}
