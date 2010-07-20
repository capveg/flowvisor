/**
 * 
 */
package org.flowvisor.ofswitch;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.LinkAdvertisement;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.events.FVTimerEvent;
import org.flowvisor.exceptions.UnhandledEvent;
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
	Map<LinkAdvertisement, Long> latestProbes;
	private boolean doCallback;
	private long updatePeriod;
	private long timeoutPeriod;

	static long defaultUpdatePeriod = 5000; // in milliseconds
	static long defaultTimeoutPeriod = 10000; // in milliseconds

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
		this.latestProbes = new HashMap<LinkAdvertisement, Long>();
		this.doCallback = false;
		this.setUpdatePeriod(TopologyController.defaultUpdatePeriod);
		// schedule the update timer
		pollLoop.addTimer(new FVTimerEvent(System.currentTimeMillis()
				+ this.updatePeriod, this, this, null));
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
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		if (e instanceof FVTimerEvent)
			processUpdate();
		else
			super.handleEvent(e);
	}

	/**
	 * On each processUpdate signal, step through list of probes,
	 * 
	 * @return
	 */
	private synchronized void processUpdate() {
		FVLog.log(LogLevel.DEBUG, this, "processing updates");
		for (Iterator<LinkAdvertisement> it = this.latestProbes.keySet()
				.iterator(); it.hasNext();) {
			LinkAdvertisement linkAdvertisement = it.next();
			long tooLate = System.currentTimeMillis() + this.timeoutPeriod;
			long thisProbe = latestProbes.get(linkAdvertisement).longValue();
			if (thisProbe > tooLate) {
				FVLog.log(LogLevel.INFO, this, "removing link "
						+ linkAdvertisement);
				FVLog.log(LogLevel.DEBUG, this, "timeout: " + thisProbe + " > "
						+ tooLate);
				this.doCallback = true;
				it.remove();
			}
		}
		if (this.doCallback)
			processCallback();
		// Schedule next event
		pollLoop.addTimer(new FVTimerEvent(System.currentTimeMillis()
				+ this.updatePeriod, this, this, null));

	}

	private void processCallback() {
		// TODO Auto-generated method stub
		FVLog.log(LogLevel.INFO, this, "topology changed: doing callbacks");
		FVLog.log(LogLevel.CRIT, this, "topology changed: IMPLEMENT CALLBACK");

		this.doCallback = false;
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

	public synchronized Set<LinkAdvertisement> getLinks() {
		return latestProbes.keySet();
	}

	public synchronized void reportProbe(LinkAdvertisement linkAdvertisement) {
		if (!this.latestProbes.containsKey(linkAdvertisement)) {
			this.doCallback = true;
			FVLog.log(LogLevel.INFO, this, "adding link " + linkAdvertisement);
		}
		this.latestProbes.put(linkAdvertisement, Long.valueOf(System
				.currentTimeMillis()));
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

	public void setUpdatePeriod(long updatePeriod) {
		this.updatePeriod = updatePeriod;
	}

	public long getUpdatePeriod() {
		return updatePeriod;
	}

	public void setTimeoutPeriod(long timeoutPeriod) {
		this.timeoutPeriod = timeoutPeriod;
	}

	public long getTimeoutPeriod() {
		return timeoutPeriod;
	}

}
