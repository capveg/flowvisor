/**
 * 
 */
package org.flowvisor.config;

import org.flowvisor.api.APIAuth;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.flows.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;

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
	final static public String LISTEN_PORT 		= "flowvisor.listen_port";
	public static final String API_WEBSERVER_PORT = "flowvisor.api_webserver_port";

	final static public String VERSION_STR		= "version";
	final static public String SLICES 			= "slices";
	final static public String SWITCHES 		= "switches";
	final static public String FLOWSPACE		= "flowspace";
	final static public String SLICE_CONTROLLER_HOSTNAME = "controller_hostname";
	final static public String SLICE_CONTROLLER_PORT = "controller_port";
	final static public String SLICE_CONTACT_EMAIL	 = "contact_email";
	public static final String SLICE_SALT = "passwd_salt";	
	public static final String SLICE_CRYPT = "passwd_crypt";	
	public static final String SLICE_CREATOR = "creator";	
	
	final static public int	   OFP_TCP_PORT	    = 6633;
	
	
	static ConfDirEntry root = new ConfDirEntry("");  // base of all config info
	
	/** 
	 * Return the config entry specific in name
	 * @param name
	 * @return null if not found
	 */
	static private ConfigEntry lookup(String name) {
		List<String> parts = Arrays.asList(name.split("\\."));
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
	
	static protected ConfigEntry create(String name, ConfigType type) throws ConfigError {
		String[] parts = name.split("\\.");
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
			entry = c.getConstructor(new Class[]{String.class}).newInstance(parts[parts.length-1]);
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
		else if(entry.type != ConfigType.INT)
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
	 * Sets an integer in the config
	 * Will dynamically create the path if it does not exist
	 * @param node e.g., "path.to.configname"
	 * @param val any integer
	 * @throws ConfigError If trying to create the path conflicted with existing config
	 */
	
	static public void setString(String node, String val) throws ConfigError {
		ConfigEntry entry = FVConfig.lookup(node);
		if (entry == null ) 
			entry = create(node, ConfigType.STR);
		else if( entry.getType() != ConfigType.STR)
			throw new ConfigWrongTypeError("tried to set an " + entry.getType() + " to string");
		ConfStrEntry ei = (ConfStrEntry)entry;
		ei.setString(val);
	}
	
	/** 
	 * Return the integer associated with this node
	 * 
	 * @param node Full path to node
	 * @return integer
	 * @throws ConfigError If entry not found or if not an int
	 */
	static public String getString(String node) throws ConfigError {
		ConfigEntry entry = FVConfig.lookup(node);
		if (entry == null)
			throw new ConfigNotFoundError("node " + node + " does not exist");
		if (entry.getType() != ConfigType.STR)
			throw new ConfigWrongTypeError("tried to get a string but got a " +  entry.getType());
		return ((ConfStrEntry)entry).getString();
	}
		
	/**
	 * Return the flowmap associated with this node
	 * @param node
	 * @return
	 * @throws ConfigError
	 */
	static public FlowMap getFlowMap(String node) throws ConfigError {
		ConfigEntry entry = FVConfig.lookup(node);
		if (entry == null)
			throw new ConfigNotFoundError("node " + node + " does not exist");
		if (entry.getType() != ConfigType.FLOWMAP)
			throw new ConfigWrongTypeError("tried to get a flowmap but got a " +  entry.getType());
		return ((ConfFlowMapEntry)entry).getFlowMap();
	}
	
	static public FlowMap getFlowSpaceFlowMap() {
		FlowMap flowMap;
		try {
			flowMap = FVConfig.getFlowMap(FVConfig.FLOWSPACE);
		} catch (ConfigError e) {
			throw new RuntimeException("WTF!?!  No FlowSpace defined!?!");
		}
		return flowMap;
	}
	
	/**
	 * Set the flowmap at this entry, creating it if it does not exist
	 * @param node
	 * @param val
	 * @throws ConfigError
	 */
	static public void setFlowMap(String node, FlowMap val) throws ConfigError {
		ConfigEntry entry = FVConfig.lookup(node);
		if (entry == null ) 
			entry = create(node, ConfigType.FLOWMAP);
		else if(entry.getType() != ConfigType.FLOWMAP)
			throw new ConfigWrongTypeError("tried to set an " + entry.getType() + " to a FlowMap");
		ConfFlowMapEntry efm = (ConfFlowMapEntry)entry;
		efm.setFlowMap(val);
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
	/**
	 * Recusively step through the config tree from the root
	 * and call walker on each non-directory node 
	 * @param walker
	 */
	
	static public void walk(ConfigIterator walker) {
		walksubdir("", root, walker);
	}

	static public void walksubdir(String base, ConfigIterator walker) {
		ConfigEntry e = lookup(base);
		walksubdir(base,e,walker);
	}
	
	static private void walksubdir(String base, ConfigEntry e, ConfigIterator walker) {
		if (e.getType() == ConfigType.DIR) {
			ConfDirEntry dir = (ConfDirEntry) e;
			for(ConfigEntry entry : dir.listEntries()) 
				walksubdir(base + "." + entry.getName(), entry, walker);
		}
		else 
			walker.visit(base,e);
		
	}

	public static void watch(FVEventHandler handler, String name)  throws ConfigError {
		ConfigEntry e = lookup(name);
		e.watch(handler);
	}
	
	public static void unwatch(FVEventHandler handler, String name)  throws ConfigError {
		ConfigEntry e = lookup(name);
		e.unwatch(handler);
	}
	
	
	/**
	 * Read XML-encoded config from filename
	 * 
	 * @param filename fully qualified or relative pathname
	 */
	public static synchronized void readFromFile(String filename) throws FileNotFoundException {
		XMLDecoder dec = new XMLDecoder(
					new BufferedInputStream(
							new FileInputStream(filename)
							)
					);
		FVConfig.root = (ConfDirEntry) dec.readObject();
	}
	
	/**
	 * Write XML-encoded config to filename
	 * 
	 * @param filename fully qualified or relative pathname
	 */
	public static synchronized void writeToFile(String filename) throws FileNotFoundException {
		XMLEncoder enc = new XMLEncoder(
					new BufferedOutputStream(
							new FileOutputStream(filename)
							)
					);
		FVConfig.walk(new ConfigDumper(System.err));
		enc.writeObject(FVConfig.root);
		enc.close();
	}
	
	public synchronized static void createSlice(
			String sliceName, 
			String controller_hostname, 
			int controller_port, 
			String passwd,
			String slice_email,
			String creatorSlice) {
		String base = FVConfig.SLICES + "."+ sliceName;
		try {
			FVConfig.create(base, ConfigType.DIR);
			FVConfig.setString(base + "." + FVConfig.SLICE_CONTACT_EMAIL, slice_email);
			FVConfig.setString(base + "." + FVConfig.SLICE_CONTROLLER_HOSTNAME, controller_hostname);
			FVConfig.setInt(base + "." + FVConfig.SLICE_CONTROLLER_PORT, controller_port);
			String salt = APIAuth.getSalt();
			FVConfig.setString(base + "." + FVConfig.SLICE_SALT, salt);
			FVConfig.setString(base + "." + FVConfig.SLICE_CRYPT, APIAuth.makeCrypt(salt, passwd));
			FVConfig.setString(base + "." + FVConfig.SLICE_CREATOR, creatorSlice);
			
		} catch (ConfigError e) {
			throw new RuntimeException("failed to create slice " + sliceName + "::" + e);
		}		
	}

	public static String readPasswd(String prompt) throws IOException { 
		System.err.println(prompt);
		// FIXME turn off echo
		return new BufferedReader(new InputStreamReader(System.in)).readLine();
	}


	public static void deleteSlice(String sliceName) throws ConfigNotFoundError{
		ConfDirEntry sliceList= (ConfDirEntry) lookup(FVConfig.SLICES);
		if(!sliceList.entries.containsKey(sliceName))
			throw new ConfigNotFoundError("slice does not exist: " + sliceName);
		sliceList.entries.remove(sliceName);
	}

	public static boolean confirm(String base) {
		return (lookup(base) != null);
	}

	/**
	 * Return the name of the super user account
	 * @return
	 */
	public static boolean isSupervisor(String user) {
		return "root".equals(user);
	}

	/**
	 * Create a default config file and write it to arg1
	 * 
	 * @param args filename
	 * @throws FileNotFoundException 
	 */
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		if(args.length != 1) {
			System.err.println("Usage: FVConfig filename");
			System.exit(1);
		}
		String filename = args[0];
		String passwd = FVConfig.readPasswd("Enter password for root account (will be echo'd!):");
		System.err.println("Generating default config to " + filename);
		DefaultConfig.init(passwd);
		FVConfig.writeToFile(filename);
	}
}
