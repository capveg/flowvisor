/**
 *
 */
package org.flowvisor.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.FlowChange.FlowChangeOp;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.BracketParse;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.InvalidSliceName;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.FlowEntryNotFound;
import org.flowvisor.exceptions.InvalidUserInfoKey;
import org.flowvisor.exceptions.MalformedControllerURL;
import org.flowvisor.exceptions.MalformedFlowChange;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.exceptions.SliceNotFound;
import org.flowvisor.flows.FlowDBEntry;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowRewriteDB;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.log.SendRecvDropStats;
import org.flowvisor.ofswitch.TopologyController;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.util.HexString;
import org.openflow.util.U16;

/**
 * This is the actual UserAPI that gets wrapped via XMLRPC In theory
 * ("God willin' and the creek dun rise"), XMLRPC calls will call these function
 * directly
 *
 * @author capveg
 *
 */
public class FVUserAPIImpl implements FVUserAPI {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * For debugging
	 *
	 * @param arg
	 *            test string
	 * @return response test string
	 */
	public String ping(String arg) {
		String user = APIUserCred.getUserName();
		return "PONG(" + user + "): FV version=" + FlowVisor.FLOWVISOR_VERSION
				+ "::" + arg;
	}

	/**
	 * Lists all the flowspace
	 *
	 * @return
	 */
	@Override
	public String[] listFlowSpace() {
		String sliceName = APIUserCred.getUserName();
		String[] fs;
		FVLog.log(LogLevel.DEBUG, null, "API listFlowSpace() by: " + sliceName);
		FlowMap flowMap;
		synchronized (FVConfig.class) {
			if (FVConfig.isSupervisor(sliceName))
				flowMap = FVConfig.getFlowSpaceFlowMap();
			else
				flowMap = FlowSpaceUtil.getSliceFlowSpace(sliceName);
			fs = new String[flowMap.countRules()];
			int i = 0;
			for (FlowEntry flowEntry : flowMap.getRules())
				fs[i++] = flowEntry.toString();
		}
		return fs;
	}

	/**
	 * Create a new slice (without flowspace)
	 *
	 * Slices that contain the field separator are rewritten with underscores
	 *
	 * @param sliceName
	 *            Cannot contain FVConfig.FS == '!'
	 * @param passwd
	 *            Cleartext! FIXME
	 * @param controller_url
	 *            Reference controller pseudo-url, e.g., tcp:hostname[:port]
	 * @param slice_email
	 *            As a contract for the slice
	 * @return success
	 * @throws InvalidSliceName
	 * @throws PermissionDeniedException
	 */
	@Override
	public boolean createSlice(String sliceName, String passwd,
			String controller_url, String slice_email)
			throws MalformedControllerURL, InvalidSliceName,
			PermissionDeniedException {
		// FIXME: make sure this user has perms to do this OP
		// for now, all slices can create other slices
		// FIXME: for now, only handle tcp, not ssl controller url
		String[] list = controller_url.split(":");
		if (!FVConfig.isSupervisor(APIUserCred.getUserName()))
			throw new PermissionDeniedException(
					"only superusers can create new slices");
		if (list.length < 2)
			throw new MalformedControllerURL(
					"controller url needs to be of the form "
							+ "proto:hostname[:port], e.g., tcp:yourhost.foo.com:6633, not: "
							+ controller_url);
		if (!list[0].equals("tcp"))
			throw new MalformedControllerURL(
					"Flowvisor currently only supports 'tcp' proto, not: "
							+ list[0]);
		int controller_port;
		if (list.length >= 3)
			controller_port = Integer.valueOf(list[2]);
		else
			controller_port = FVConfig.OFP_TCP_PORT;
		// createSlice is synchronized()

		// We need to make sure this slice doesn't already exist
		List<String> slices = null;
		synchronized (FVConfig.class) {
			try {
				slices = FVConfig.list(FVConfig.SLICES);
			} catch (ConfigError e) {
				e.printStackTrace();
				throw new RuntimeException("no SLICES subdir found in config");
			}
			for (Iterator<String> sliceIter = slices.iterator(); sliceIter
					.hasNext();) {
				if (sliceName.equals(sliceIter.next())) {
					throw new PermissionDeniedException(
							"Cannot create slice with existing name.");
				}
			}
		}

		FVConfig.createSlice(sliceName, list[1], controller_port, passwd,
				slice_email, APIUserCred.getUserName());
		FlowVisor.getInstance().checkPointConfig();
		return true;
	}

	/**
	 * Change the password for this slice
	 *
	 * A slice is allowed to change its own password and the password of any
	 * slice that it has (transitively) created
	 *
	 * @param sliceName
	 * @param newPasswd
	 */
	@Override
	public boolean changePasswd(String sliceName, String newPasswd)
			throws PermissionDeniedException {
		String changerSlice = APIUserCred.getUserName();
		if (!APIAuth.transitivelyCreated(changerSlice, sliceName)
				&& !FVConfig.isSupervisor(changerSlice))
			throw new PermissionDeniedException("Slice " + changerSlice
					+ " does not have perms to change the passwd of "
					+ sliceName);
		String salt = APIAuth.getSalt();
		String crypt = APIAuth.makeCrypt(salt, newPasswd);
		sliceName = FVConfig.sanitize(sliceName);
		// set passwd is synchronized
		FVConfig.setPasswd(sliceName, salt, crypt);
		FlowVisor.getInstance().checkPointConfig();
		return true;
	}

	@Override
	public boolean changeSlice(String sliceName, String key, String value)
			throws MalformedURLException, InvalidSliceName,
			PermissionDeniedException, InvalidUserInfoKey {
		String changerSlice = APIUserCred.getUserName();
		if (!APIAuth.transitivelyCreated(changerSlice, sliceName)
				&& !FVConfig.isSupervisor(changerSlice))
			throw new PermissionDeniedException("Slice " + changerSlice
					+ " does not have perms to change the passwd of "
					+ sliceName);
		/**
		 * this is the list of things a user is allowed to change about
		 * themselves. Critically, it should not include "creator" string as
		 * this would allow security issues.
		 */

		String base = FVConfig.SLICES + FVConfig.FS + sliceName + FVConfig.FS
				+ key;
		try {
			if (key.equals("contact_email") || key.equals("controller_hostname"))
				FVConfig.setString(base, value);
			else if (key.equals("controller_port"))
				FVConfig.setInt(base, Integer.valueOf(value));
			else
				throw new InvalidUserInfoKey("invalid key: " + key
						+ "-- only contact_email and "
						+ "controller_{hostname,port} can be changed");
			FlowVisor.getInstance().checkPointConfig();
		} catch (ConfigError e) {
			// this should probably never happen b/c of above checks
			throw new InvalidUserInfoKey(e.toString());
		}

		return true;
	}

	@Override
	public boolean change_password(String sliceName, String newPasswd)
			throws PermissionDeniedException {
		return changePasswd(sliceName, newPasswd);
		// just call changePasswd(); keeping the two names made things easier
		// for Jad /shrug
	}

	/**
	 * For now, create a circular, bidirectional loop between existing switches
	 * FIXME need to actually infer and calc real topology
	 */

	@Override
	public List<Map<String, String>> getLinks() {
		FVLog.log(LogLevel.DEBUG, null,
				"API getLinks() by: " + APIUserCred.getUserName());
		TopologyController topologyController = TopologyController
				.getRunningInstance();
		if (topologyController == null)
			return getFakeLinks();
		List<Map<String, String>> list = new LinkedList<Map<String, String>>();
		for (Iterator<LinkAdvertisement> it = topologyController.getLinks()
				.iterator(); it.hasNext();) {
			LinkAdvertisement linkAdvertisement = it.next();
			list.add(linkAdvertisement.toMap());
		}
		return list;
	}

	protected List<Map<String, String>> getFakeLinks() {
		FVLog.log(LogLevel.ALERT, null,
				"API: topology server not running: faking getLinks()");
		List<String> devices = listDevices();
		List<Map<String, String>> list = new LinkedList<Map<String, String>>();
		for (int i = 0; i < devices.size(); i++) {
			// forward direction
			LinkAdvertisement link = new LinkAdvertisement();
			link.srcDPID = devices.get(i);
			link.dstDPID = devices.get((i + 1) % devices.size());
			link.srcPort = 0;
			link.dstPort = 1;
			link.setAttribute("fakeLink", "true");
			list.add(link.toMap());
		}
		return list;
	}

	@Override
	public List<String> listDevices() {
		FVLog.log(LogLevel.DEBUG, null,
				"API listDevices() by: " + APIUserCred.getUserName());
		FlowVisor fv = FlowVisor.getInstance();
		// get list from main flowvisor instance
		List<String> dpids = new ArrayList<String>();
		String dpidStr;

		/*
		 * if (TopologyController.isConfigured()) { for (Long dpid :
		 * TopologyController.getRunningInstance() .listDevices()) { dpidStr =
		 * HexString.toHexString(dpid); if (!dpids.contains(dpidStr))
		 * dpids.add(dpidStr); else FVLog.log(LogLevel.WARN, TopologyController
		 * .getRunningInstance(), "duplicate dpid detected: " + dpidStr); } }
		 * else {
		 */
		// only list a device is we have a features reply for it
		for (FVEventHandler handler : fv.getHandlersCopy()) {
			if (handler instanceof FVClassifier) {
				OFFeaturesReply featuresReply = ((FVClassifier) handler)
						.getSwitchInfo();
				if (featuresReply != null) {
					dpidStr = FlowSpaceUtil.dpidToString(featuresReply
							.getDatapathId());
					if (!dpids.contains(dpidStr))
						dpids.add(dpidStr);
					else
						FVLog.log(LogLevel.WARN, handler,
								"duplicate dpid detected: " + dpidStr);
				}
			}
		}
		// }
		return dpids;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.api.FVUserAPI#getDeviceInfo()
	 */
	@Override
	public Map<String, String> getDeviceInfo(String dpidStr)
			throws DPIDNotFound {
		Map<String, String> map = new HashMap<String, String>();
		long dpid = HexString.toLong(dpidStr);
		FVClassifier fvClassifier = null;
		for (FVEventHandler handler : FlowVisor.getInstance().getHandlersCopy()) {
			if (handler instanceof FVClassifier) {
				OFFeaturesReply featuresReply = ((FVClassifier) handler)
						.getSwitchInfo();
				if (featuresReply != null
						&& featuresReply.getDatapathId() == dpid) {
					fvClassifier = (FVClassifier) handler;
					break;
				}
			}
		}
		if (fvClassifier == null)
			throw new DPIDNotFound("dpid does not exist: " + dpidStr + " ::"
					+ String.valueOf(dpid));
		OFFeaturesReply config = fvClassifier.getSwitchInfo();
		map.put("dpid", FlowSpaceUtil.dpidToString(dpid));
		if (config != null) {
			map.put("nPorts", String.valueOf(config.getPorts().size()));
			String portList = "";
			String portNames = "";
			int p;
			for (Iterator<OFPhysicalPort> it = config.getPorts().iterator(); it
					.hasNext();) {
				OFPhysicalPort port = it.next();
				p = U16.f(port.getPortNumber());
				portList += p;
				portNames += port.getName() + "(" + p + ")";
				if (it.hasNext()) {
					portList += ",";
					portNames += ",";
				}
			}
			map.put("portList", portList);
			map.put("portNames", portNames);
		} else {
			FVLog.log(LogLevel.WARN, null, "null config for: " + dpidStr);
		}
		map.put("remote", String.valueOf(fvClassifier.getConnectionName()));
		return map;
	}

	@Override
	public boolean deleteSlice(String sliceName) throws SliceNotFound,
			PermissionDeniedException {
		String changerSlice = APIUserCred.getUserName();
		if (!APIAuth.transitivelyCreated(changerSlice, sliceName)) {
			FVLog.log(LogLevel.WARN, null, "API deletSlice(" + sliceName
					+ ") failed by: " + APIUserCred.getUserName());
			throw new PermissionDeniedException("Slice " + changerSlice
					+ " does not have perms to change the passwd of "
					+ sliceName);
		}
		synchronized (FVConfig.class) {
			try {
				// this is also synchronized against FVConfig.class
				FVConfig.deleteSlice(sliceName);
			} catch (Exception e) {
				throw new SliceNotFound("slice does not exist: " + sliceName);
			}
			FVLog.log(LogLevel.DEBUG, null, "API removeSlice(" + sliceName
					+ ") by: " + APIUserCred.getUserName());
			FlowSpaceUtil.deleteFlowSpaceBySlice(sliceName);
			FVConfig.sendUpdates(FVConfig.FLOWSPACE);
			// signal that FS has changed
			FlowVisor.getInstance().checkPointConfig();
		}
		return true;
	}

	/**
	 * Implements {@link org.flowvisor.api.FVUserAPI#changeFlowSpace}
	 *
	 * Allow this change if it affectst the flowspace delagated to this slice.
	 *
	 * @throws PermissionDeniedException
	 *
	 */

	@Override
	public List<String> changeFlowSpace(List<Map<String, String>> changes)
			throws MalformedFlowChange, PermissionDeniedException,
			FlowEntryNotFound {
		String user = APIUserCred.getUserName();
		List<String> returnIDs = new LinkedList<String>();

		// TODO: implement the "delegate" bit; for now only root can change FS
		if (!FVConfig.isSupervisor(APIUserCred.getUserName()))
			throw new PermissionDeniedException(
					"only superusers can add/remove/change the flowspace");

		synchronized (FVConfig.class) { // prevent multiple API clients from
			// stomping
			// on each other
			FlowMap flowSpace = FVConfig.getFlowSpaceFlowMap();
			String logMsg;
			for (int i = 0; i < changes.size(); i++) {
				FlowChange change = FlowChange.fromMap(changes.get(i));
				FlowChangeOp operation = change.getOperation();
				logMsg = "user " + user + " " + operation;
				if (operation != FlowChangeOp.ADD) {
					logMsg += " id=" + change.getId();
					flowSpace.removeRule(change.getId());
					returnIDs.add(String.valueOf(change.getId()));
				}
				if (operation != FlowChangeOp.REMOVE) {
					logMsg += " for dpid="
							+ FlowSpaceUtil.dpidToString(change.getDpid())
							+ " match=" + change.getMatch() + " priority="
							+ change.getPriority() + " actions="
							+ FlowSpaceUtil.toString(change.getActions());

					FlowEntry flowEntry = new FlowEntry(change.getDpid(),
							change.getMatch(), change.getPriority(),
							change.getActions());

					if (operation == FlowChangeOp.ADD)
						returnIDs.add(String.valueOf(flowEntry.getId()));
					else
						flowEntry.setId(change.getId()); // keep id on change
					flowSpace.addRule(flowEntry);

				}
				FVLog.log(LogLevel.INFO, null, logMsg);
			}
			// update the indexes at the end, not with each rule
			FlowVisor.getInstance().checkPointConfig();
			FVLog.log(LogLevel.INFO, null,
					"Signalling FlowSpace Update to all event handlers");
			FVConfig.sendUpdates(FVConfig.FLOWSPACE); // signal that FS has
			// changed
		}
		return returnIDs;
	}

	@Override
	public List<String> listSlices() throws PermissionDeniedException {
		/*
		 * relaxed security; anyone can get a list of slices if
		 * (!FVConfig.isSupervisor(APIUserCred.getUserName())) throw new
		 * PermissionDeniedException( "listSlices only available to root");
		 */
		List<String> slices = null;
		synchronized (FVConfig.class) {
			try {
				// this is synchronized
				List<String> entries = FVConfig.list(FVConfig.SLICES);
				slices = new LinkedList<String>(entries);
			} catch (ConfigError e) {
				e.printStackTrace();
				throw new RuntimeException(
						"wtf!?: no SLICES subdir found in config");
			}
		}
		return slices;
	}

	@Override
	public Map<String, String> getSliceInfo(String sliceName)
			throws PermissionDeniedException {
		HashMap<String, String> map = new HashMap<String, String>();
		/*
		 * relaxed security -- anyone can read slice info for now String user =
		 * APIUserCred.getUserName(); if (!FVConfig.isSupervisor(user) &&
		 * !APIAuth.transitivelyCreated(user, sliceName)) throw new
		 * PermissionDeniedException(
		 * "not superuser or transitive slice creator");
		 */
		String base = FVConfig.SLICES + FVConfig.FS + sliceName + FVConfig.FS;

		synchronized (FVConfig.class) {
			try {
				map.put("contact_email",
						FVConfig.getString(base + "contact_email"));
				map.put("controller_hostname",
						FVConfig.getString(base + "controller_hostname"));
				map.put("controller_port", String.valueOf(FVConfig.getInt(base
						+ "controller_port")));
				map.put("creator", FVConfig.getString(base + "creator"));
			} catch (ConfigError e) {
				FVLog.log(LogLevel.CRIT, null, "malformed slice: " + e);
				e.printStackTrace();
			}
		}
		long dpid;
		int connection = 1;

		// TODO: come back an architect this so we can walk the list of slicers,
		// not the list of classifiers, and then slicers
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (!classifier.isIdentified()) // only print switches have have
					// been identified
					continue;
				dpid = classifier.getDPID();
				FVSlicer fvSlicer = classifier.getSlicerByName(sliceName);
				if (fvSlicer != null) {
					map.put("connection_" + connection++,
							FlowSpaceUtil.dpidToString(dpid) + "-->"
									+ fvSlicer.getConnectionName());
				}

			}
		}

		return map;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.api.FVUserAPI#revertToLastCheckpoint()
	 */
	@Override
	public boolean revertToLastCheckpoint() {
		// TODO: implement!
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.api.FVUserAPI#getConfig(java.lang.String)
	 */
	@Override
	public List<String> getConfig(String nodeName) throws ConfigError,
			PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!FVConfig.isSupervisor(user)) {
			FVLog.log(LogLevel.WARN, null, "blocked getConfig for user " + user
					+ " on config " + nodeName);
			throw new PermissionDeniedException(
					"only superusers can call getConfig()");
		}
		FVLog.log(LogLevel.DEBUG, null, "getConfig for user " + user
				+ " on config " + nodeName);
		// this is synchronized against FVConfig
		return FVConfig.getConfig(nodeName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.flowvisor.api.FVUserAPI#setConfig(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public boolean setConfig(String nodeName, String value) throws ConfigError,
			PermissionDeniedException {
		String user = APIUserCred.getUserName();
		if (!FVConfig.isSupervisor(user)) {
			FVLog.log(LogLevel.WARN, null, "blocked setConfig for user " + user
					+ " on config " + nodeName + " to " + value);
			throw new PermissionDeniedException(
					"only superusers can call setConfig()");
		}
		// this is synchronized against FVConfig
		FVConfig.setConfig(nodeName, value);
		FVConfig.sendUpdates(nodeName);
		FlowVisor.getInstance().checkPointConfig();
		FVLog.log(LogLevel.DEBUG, null, "setConfig for user " + user
				+ " on config " + nodeName + " to " + value);

		return true;
	}

	@Override
	public boolean registerTopologyChangeCallback(String URL, String cookie)
			throws MalformedURLException {
		// this will throw MalformedURL back to the client if the URL is bad
		new URL(URL);
		TopologyController tc = TopologyController.getRunningInstance();
		if (tc != null) {
			tc.registerCallBack(APIUserCred.getUserName(), URL, cookie);
			return true;
		} else
			return false; // topology server not running
	}

	@Override
	public boolean unregisterTopologyChangeCallback() {
		TopologyController tc = TopologyController.getRunningInstance();
		if (tc != null) {
			tc.unregisterCallBack(APIUserCred.getUserName());
			return true;
		} else
			return false; // topology server not running
	}

	@Override
	public String getSliceStats(String sliceName) throws SliceNotFound,
			PermissionDeniedException {

		List<String> slices = new ArrayList<String>();
		try {
			slices = FVConfig.list(FVConfig.SLICES);
		} catch (ConfigError e) {
			e.printStackTrace();
		}
		if (!(slices.contains(sliceName))){
			throw new SliceNotFound("Slice does not exist: " + sliceName);
		}

		FVSlicer fvSlicer = null;
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (!classifier.isIdentified()) // only print switches have have
					// been identified
					continue;
				fvSlicer = classifier.getSlicerByName(sliceName);
				if (fvSlicer != null) {
					break;
				}
			}
		}

		if (fvSlicer == null)
			return SendRecvDropStats.NO_STATS_AVAILABLE_MSG;

		return fvSlicer.getStats().combinedString();
	}

	@Override
	public String getSwitchStats(String dpidStr) throws DPIDNotFound,
			PermissionDeniedException {
		long dpid = FlowSpaceUtil.parseDPID(dpidStr);
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (classifier.getDPID() == dpid)
					return classifier.getStats().combinedString();
			}
		}
		throw new DPIDNotFound("dpid not found: " + dpidStr);
	}

	@Override
	public List<Map<String, String>> getSwitchFlowDB(String dpidStr)
			throws DPIDNotFound {
		boolean found = false;
		long dpid = FlowSpaceUtil.parseDPID(dpidStr);
		List<Map<String, String>> ret = new LinkedList<Map<String, String>>();
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (dpid == classifier.getDPID() || dpid == FlowEntry.ALL_DPIDS) {
					synchronized (classifier) {
						for (Iterator<FlowDBEntry> it2 = classifier.getFlowDB()
								.iterator(); it2.hasNext();) {
							ret.add(it2.next().toBracketMap());
						}
					}
					found = true;
				}
			}
		}
		if (!found)
			throw new DPIDNotFound("dpid not found: " + dpidStr);
		return ret;
	}

	@Override
	public Map<String, List<Map<String, String>>> getSliceRewriteDB(
			String sliceName, String dpidStr) throws DPIDNotFound,
			SliceNotFound, PermissionDeniedException {
		long dpid = FlowSpaceUtil.parseDPID(dpidStr);
		FVSlicer fvSlicer = lookupSlicer(sliceName, dpid);
		Map<String, List<Map<String, String>>> ret = new HashMap<String, List<Map<String, String>>>();
		FlowRewriteDB flowRewriteDB = fvSlicer.getFlowRewriteDB();
		synchronized (flowRewriteDB) {
			for (FlowDBEntry original : flowRewriteDB.originals()) {
				Map<String, String> originalMap = original.toBracketMap();
				List<Map<String, String>> rewrites = new LinkedList<Map<String, String>>();
				for (FlowDBEntry rewrite : flowRewriteDB.getRewrites(original)) {
					rewrites.add(rewrite.toBracketMap());
				}
				ret.put(BracketParse.encode(originalMap), rewrites);
			}
		}
		return ret;
	}

	/**
	 *
	 * @param sliceName
	 * @param dpid
	 * @return a valid fvSlicer (never null)
	 * @throws DPIDNotFound
	 * @throws SliceNotFound
	 */

	private FVSlicer lookupSlicer(String sliceName, long dpid)
			throws DPIDNotFound, SliceNotFound {

		FVClassifier fvClassifier = lookupClassifier(dpid); // throws dpid not
															// found
		synchronized (fvClassifier) {
			FVSlicer fvSlicer = fvClassifier.getSlicerByName(sliceName);
			if (fvSlicer == null)
				throw new SliceNotFound(sliceName);
			return fvSlicer;
		}
	}

	/**
	 * Returns a valid fvClassifier
	 *
	 * @param dpid
	 * @return never null
	 * @throws DPIDNotFound
	 */
	private FVClassifier lookupClassifier(long dpid) throws DPIDNotFound {
		for (Iterator<FVEventHandler> it = FlowVisor.getInstance()
				.getHandlersCopy().iterator(); it.hasNext();) {
			FVEventHandler eventHandler = it.next();
			if (eventHandler instanceof FVClassifier) {
				FVClassifier classifier = (FVClassifier) eventHandler;
				if (dpid == classifier.getDPID())
					return classifier;
			}
		}
		throw new DPIDNotFound("No such switch: " + dpid);
	}
}
