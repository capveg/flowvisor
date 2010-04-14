package org.flowvisor.config;




import org.flowvisor.FlowVisor;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.LinearFlowMap;
import org.flowvisor.flows.SliceAction;
import org.openflow.protocol.OFMatch;


/**
 * List of things to populate FVConfig with on startup
 * Everything here can be overridden from the config file
 * @author capveg
 *
 */
public class DefaultConfig {

			
	static public void init() {
		// setup a bunch of default things in the config
		FlowMap flowMap = new LinearFlowMap();
		SliceAction aliceAction = new SliceAction("alice", SliceAction.WRITE);
		SliceAction bobAction = new SliceAction("bob", SliceAction.WRITE);
		OFMatch match = new OFMatch();
		short alicePorts[] = { 0, 2, 3 };
		String aliceMacs[] = {"00:00:00:00:00:02", "00:01:00:00:00:02"};
		// short alicePorts[] = { 2 };
		// String aliceMacs[] = {"00:01:00:00:00:02"};

		
		short bobPorts[]  = { 1, 3};
		String bobMacs[] = { "00:00:00:00:00:01", "00:01:00:00:00:01"};
		// short bobPorts[]  = { 3};
		// String bobMacs[] = { "00:01:00:00:00:01"};
		
		int position=0;		
		int i,j;
		match.setWildcards(OFMatch.OFPFW_ALL & ~(OFMatch.OFPFW_DL_SRC|OFMatch.OFPFW_IN_PORT));
		
		// add all of alice's rules
		for(i=0; i< alicePorts.length; i++) {
			match.setInputPort(alicePorts[i]);
			for(j=0; j < aliceMacs.length ; j++) {
				match.setDataLayerSource(aliceMacs[j]);
				flowMap.addRule(position++, new FlowEntry(match.clone(), aliceAction));
			}
		}
		
		// add all of bob's rules
		for(i=0; i< bobPorts.length; i++) {
			match.setInputPort(bobPorts[i]);
			for(j=0; j < bobMacs.length ; j++) {
				match.setDataLayerSource(bobMacs[j]);
				flowMap.addRule(position++, new FlowEntry(match.clone(), bobAction));
			}
		}
			

		// now populate the config
		try {
			FVConfig.setInt(FVConfig.LISTEN_PORT, FVConfig.OFP_TCP_PORT);
			FVConfig.setString(FVConfig.VERSION_STR, FlowVisor.FLOVISOR_VERSION);
			// create slices
			FVConfig.create(FVConfig.SLICES, ConfigType.DIR);
			// create alice slice
			String aliceSlice = FVConfig.SLICES + ".alice";
			FVConfig.create(aliceSlice, ConfigType.DIR);
			FVConfig.setString(aliceSlice + ".contact_email", "alice@foo.com");
			FVConfig.setString(aliceSlice + ".controller_hostname", "localhost");
			FVConfig.setInt(aliceSlice + ".controller_port", 54321);
			String bobSlice = FVConfig.SLICES + ".bob";
			FVConfig.create(bobSlice, ConfigType.DIR);
			FVConfig.setString(bobSlice + ".contact_email", "bob@foo.com");
			FVConfig.setString(bobSlice + ".controller_hostname", "localhost");
			FVConfig.setInt(bobSlice + ".controller_port", 54322);
			
			// create switches
			FVConfig.create(FVConfig.SWITCHES, ConfigType.DIR);
			FVConfig.setFlowMap(FVConfig.FLOWSPACE, flowMap);
		} catch (ConfigError e) {
			e.printStackTrace();
			throw new RuntimeException((Exception) e);
		}		
	}
	/**
	 * Print default config to stdout
	 * @param args
	 */
	
	
	public static void main(String args[]) {
	
		DefaultConfig.init();
		FVConfig.walk(new ConfigDumper(System.out));
	}
}
