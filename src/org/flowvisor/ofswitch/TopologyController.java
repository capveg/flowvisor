/**
 * 
 */
package org.flowvisor.ofswitch;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.LinkAdvertisement;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * A simple OpenFlow controller that runs inside the flowvisor to discover and
 * report the network's topology
 * 
 * @author capveg
 * 
 */
public class TopologyController extends OFSwitchAcceptor {

	static TopologyController runningInstance = null;
	public static String TopoUser = "root";
	List<TopologyConnection> topologyConnections;

	public static TopologyController getRunningInstance() {
		return TopologyController.runningInstance;
	}

	public static void setRunningInstance(TopologyController tc) {
		TopologyController.runningInstance = tc;
	}

	private TopologyController(FVEventLoop pollLoop, int port, int backlog)
			throws IOException {
		super(pollLoop, port, backlog);
		this.topologyConnections = new LinkedList<TopologyConnection>();
		TopologyController.setRunningInstance(this);
	}

	public static FVEventHandler spawn(FVEventLoop pollLoop) {
		if (runningInstance != null)
			return runningInstance; // return version that's already running
		if (!isConfigured())
			return null; // not configured for it
		TopologyController tc = null;
		try {
			tc = new TopologyController(pollLoop, 0, 16); // 0 == any port
			int port = tc.getListenPort();
			String base = FVConfig.SLICES + FVConfig.FS + TopoUser
					+ FVConfig.FS;
			try {
				FVConfig.setString(base + FVConfig.SLICE_CONTROLLER_HOSTNAME,
						"localhost");
				FVConfig.setInt(base + FVConfig.SLICE_CONTROLLER_PORT, port);
			} catch (ConfigError e) {
				FVLog.log(LogLevel.CRIT, tc,
						"tried to register topology controller info, but topo user '"
								+ TopoUser + "' not found: " + e);
			}
		} catch (IOException e) {
			FVLog.log(LogLevel.ALERT, null,
					"failed to spawn TopologyController: " + e);
		}
		return tc;
	}

	@Override
	public String getName() {
		return "TopoDiscovery";
	}

	@Override
	void handleIOEvent(FVIOEvent event) {
		SocketChannel sock = null;

		try {
			sock = ssc.accept();
			if (sock == null) {
				FVLog.log(LogLevel.CRIT, null,
						"ssc.accept() returned null !?! FIXME!");
				return;
			}
			FVLog.log(LogLevel.INFO, this, "got new connection: " + sock);
			TopologyConnection tc = new TopologyConnection(this, pollLoop, sock);
			tc.init();
			topologyConnections.add(tc);
		} catch (IOException e) // ignore IOExceptions -- is this the right
		// thing to do?
		{
			System.err.println("Got IOException for " + sock != null ? sock
					: "unknown socket");
			System.err.println(e);
		}
	}

	/**
	 * Return a list of datapath ids (encoded as raw longs) of all the
	 * switches/devices connected to this topology controller
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
	 * Return a list of links (encoded as LinkAdvertisements) of all discovered
	 * links of all switches connected to this topology controller.
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

	/**
	 * Returns true if the FV is configured to do topology discovery and it's
	 * running
	 * 
	 * @return true == yes, the topology discovery process is running
	 */

	public static boolean isConfigured() {
		try {
			return FVConfig.getBoolean(FVConfig.TOPOLOGY_SERVER);
		} catch (ConfigError e) {
			FVLog.log(LogLevel.WARN, null, "Creating config entry "
					+ FVConfig.TOPOLOGY_SERVER + "=false");
			try {
				FVConfig.setBoolean(FVConfig.TOPOLOGY_SERVER, false);
			} catch (ConfigError e1) {
				throw new RuntimeException(e1);
			}
			FlowVisor.getInstance().checkPointConfig();
			return false;
		}
	}

	/**
	 * @return the topoUser
	 */
	public static String getTopoUser() {
		return TopoUser;
	}

	/**
	 * @param topoUser
	 *            the topoUser to set
	 */
	public static void setTopoUser(String topoUser) {
		TopoUser = topoUser;
	}

}
