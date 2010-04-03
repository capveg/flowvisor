package org.flowvisor.config;

/**
 * List of things to populate FVConfig with on startup
 * Everything here can be overridden from the config file
 * @author capveg
 *
 */
public class DefaultConfig {
	final static public String LISTEN_PORT 		= "flowvisor.listen_port";
	final static public String SLICES 			= "slices";
	final static public String SWITCHES 		= "switches";
	final static public String FLOWSPACE		= "flowspace";
	final static public int	   OFP_TCP_PORT	    = 6633;	
			
	static public void init() {
		// setup a bunch of default things in the config
		try {
			FVConfig.setInt(LISTEN_PORT, OFP_TCP_PORT);
			FVConfig.create(SLICES, ConfigType.DIR);
			FVConfig.create(SWITCHES, ConfigType.DIR);
			FVConfig.create(FLOWSPACE, ConfigType.DIR);
		} catch (ConfigError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException((Exception) e);
		}		
	}
}
