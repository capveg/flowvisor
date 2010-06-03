/**
 * 
 */
package org.flowvisor.ofswitch;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;


import org.flowvisor.api.LinkAdvertisement;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * A simple OpenFlow controller that runs inside the flowvisor to discover and report
 * the network's topology
 * 
 * @author capveg
 *
 */
public class TopologyController extends OFSwitchAcceptor {
	
	static TopologyController runningInstance = null;
	
	public static TopologyController getRunningInstance() {
		return TopologyController.runningInstance;
	}
	
	public static void setRunningInstance( TopologyController tc) {
		TopologyController.runningInstance = tc;
	}
    
	List<TopologyConnection> topologyConnections;
	public TopologyController(FVEventLoop pollLoop, int port, int backlog)
			throws IOException {
		super(pollLoop, port, backlog);
		this.topologyConnections = new LinkedList<TopologyConnection>();
	}

	@Override
	public String getName() {
		return "TopoDiscovery";
	}
	
	@Override
    void handleIOEvent(FVIOEvent event)
    {
 		SocketChannel sock = null;

    	try
    	{
    		sock = ssc.accept();
    		if (sock == null ) {
    			FVLog.log(LogLevel.CRIT, null, "ssc.accept() returned null !?! FIXME!");
    			return;
    		}
    		FVLog.log(LogLevel.INFO, this, "got new connection: " + sock);
    		TopologyConnection tc = new TopologyConnection( this, pollLoop, sock);
      		tc.init();
    		topologyConnections.add(tc);
    	}
    	catch (IOException e)		// ignore IOExceptions -- is this the right thing to do?
    	{
    		System.err.println("Got IOException for " + sock!= null? sock : "unknown socket");
    		System.err.println(e);
    	}
    }
	
	/** 
	 * Return a list of datapath ids (encoded as raw longs) of all the switches/devices connected
	 * to this topology controller
	 * 
	 * Used by FVUserAPI getDevices() call
	 * 	  
	 * @return never null, may return an empty list if nothing is connected
	 */
	
	public List<Long> listDevices() {
		List<Long> dpids = new LinkedList<Long>();
		for (TopologyConnection tc : this.topologyConnections) 
			if (tc.isConnected())
				dpids.add(tc.getDataPathID());
		return dpids;
	}
	
	/**
	 * Return a list of links (encoded as LinkAdvertisements) of all discovered links of all switches
	 * connected to this topology controller.
	 * 
	 * Used by FVUserAPI getLinks() call
	 * 
	 * @return
	 */
	
	public List<LinkAdvertisement> getLinks() {
		List<LinkAdvertisement> links = new LinkedList<LinkAdvertisement>();
		for (TopologyConnection tc : this.topologyConnections)
			if (tc.isConnected())
				tc.getLinks(links);
		return links;
	}

}
