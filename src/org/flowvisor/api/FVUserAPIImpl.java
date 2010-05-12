/**
 * 
 */
package org.flowvisor.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.APIUserCred;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.MalformedControllerURL;
import org.flowvisor.exceptions.MalformedFlowChange;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.exceptions.SliceNotFound;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.util.HexString;

/**
 * This is the actual UserAPI that gets wrapped via XMLRPC
 * In theory ("God willin' and the creek dun rise"), XMLRPC
 * calls will call these function directly
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
	 * @param arg test string
	 * @return response test string
	 */
	public String ping(String arg) {
		String user = APIUserCred.getUserName();
		return "PONG(" + user + "): " + arg;
	}
	
	/**
	 * Lists all the flowspace
	 * 
	 * @return
	 */
	@Override
	public String[] listFlowSpace() {
		String sliceName = APIUserCred.getUserName();
		FlowMap flowMap;
		if (FVConfig.isSupervisor(sliceName))
			flowMap = FVConfig.getFlowSpaceFlowMap();
		else
			flowMap = FlowSpaceUtil.getSliceFlowSpace(sliceName);
		String[] fs = new String[flowMap.countRules()];
		for(int i=0; i< fs.length; i++)
			fs[i] = flowMap.getRules().get(i).toString();
		return fs;
	}
	
	/**
	 * Create a new slice (without flowspace)
	 * 
	 * @param sliceName
	 * @param passwd Cleartext! FIXME
	 * @param controller_url Reference controller pseudo-url, e.g., tcp:hostname[:port]
	 * @param slice_email As a contract for the slice
	 * @return success
	 */
	@Override
	public boolean createSlice(String sliceName, String passwd, 
			String controller_url, String slice_email) throws MalformedControllerURL {
		// FIXME: make sure this user has perms to do this OP
		// for now, all slices can create other slices
		// FIXME: for now, only handle tcp, not ssl controller url
		String[] list = controller_url.split(":");
		if(list.length< 2)
			throw new MalformedControllerURL("controller url needs to be of the form " +
					"proto:hostname[:port], e.g., tcp:yourhost.foo.com:6633, not: " + controller_url);
		if(!list[0].equals("tcp"))
			throw new MalformedControllerURL("Flowvisor currently only supports 'tcp' proto, not: "
					+ list[0]);
		int controller_port;
		if (list.length>=3)
			controller_port = Integer.valueOf(list[2]);
		else
			controller_port = FVConfig.OFP_TCP_PORT;
		FVConfig.createSlice(sliceName, list[1], controller_port, passwd, slice_email, 
				APIUserCred.getUserName());
		return true;
	}
	
	/**
	 * Change the password for this slice
	 * 
	 * A slice is allowed to change its own password and the password
	 * of any slice that it has (transitively) created
	 * 
	 * @param sliceName
	 * @param newPasswd
	 */
	@Override
	public boolean changePasswd(String sliceName, String newPasswd) throws PermissionDeniedException {
		String changerSlice = APIUserCred.getUserName();
		if(!APIAuth.transitivelyCreated(changerSlice,sliceName))
			throw new PermissionDeniedException("Slice " + changerSlice + 
					" does not have perms to change the passwd of " + sliceName); 
		String salt = APIAuth.getSalt();
		String crypt = APIAuth.makeCrypt(salt, newPasswd);
		String base = FVConfig.SLICES + "." + sliceName;
		try {
			FVConfig.setString(base + "." + FVConfig.SLICE_SALT, salt);
			FVConfig.setString(base + "." + FVConfig.SLICE_CRYPT, crypt);
			
		} catch (ConfigError e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return true;
	}

	@Override
	public boolean change_passwd(String sliceName, String newPasswd)
			throws PermissionDeniedException {
		return changePasswd(sliceName, newPasswd);
		// just call changePasswd(); keeping the two names made things easier for Jad /shrug
	}

	/**
	 * For now, create a circular, bidirectional loop between existing switches
	 * FIXME need to actually infer and calc real topology
	 */
	
	@Override
	public LinkAdvertisement[] getLinks() {
		String[] devices = getDevices();
		LinkAdvertisement[] list = new LinkAdvertisement[devices.length*2];
		int linkIndex;
		for(int i=0;i<devices.length; i++) {
			// forward direction
			linkIndex=i*2;
			list[linkIndex].srcDPID = devices[i];
			list[linkIndex].dstDPID = devices[(i+1)%devices.length];
			list[linkIndex].srcPort = 0;
			list[linkIndex].dstPort = 1;
			list[linkIndex].attributes.put("fakeLink", "true");
			// reverse direction
			linkIndex=i*2+1;
			list[linkIndex].dstDPID = devices[i];
			list[linkIndex].srcDPID = devices[(i+1)%devices.length];
			list[linkIndex].dstPort = 0;
			list[linkIndex].srcPort = 1;
			list[linkIndex].attributes.put("fakeLink", "true");
		}
		return list;
	}
	
	@Override
	public String[] getDevices(){
		FlowVisor fv = FlowVisor.getInstance();
		// get list from main flowvisor instance
		List<Long> dpids = new ArrayList<Long>();
		for(FVEventHandler handler : fv.getHandlers()) {
			if(handler instanceof FVClassifier)
				dpids.add(((FVClassifier) handler).getSwitchInfo().getDatapathId());
		}
		String arr[] = new String[dpids.size()];
		int i=0;
		for(Long dpid : dpids)
			arr[i++] = HexString.toHexString(dpid.longValue());
		return arr;
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.flowvisor.api.FVUserAPI#getDeviceInfo()
	 */
	@Override
	public Map<String, String> getDeviceInfo(String dpidStr) throws DPIDNotFound {
		Map<String,String> map = new HashMap<String,String>();
		long dpid = HexString.toLong(dpidStr);
		FVClassifier fvClassifier = null;
		for(FVEventHandler handler: FlowVisor.getInstance().getHandlers()) {
			if(handler instanceof FVClassifier) {
				if (((FVClassifier)handler).getSwitchInfo().getDatapathId() == dpid) {
					fvClassifier = (FVClassifier) handler;
					break;
				}
			}
		}
		if (fvClassifier == null)
			throw new DPIDNotFound("dpid does not exist: " +  dpidStr + " ::" +String.valueOf(dpid));
		OFFeaturesReply config = fvClassifier.getSwitchInfo();
		map.put("dpid", String.valueOf(dpid));
		map.put("nPorts",String.valueOf(config.getPorts().size()));
		map.put("remote", String.valueOf(fvClassifier.getRemoteIP()));
		return map;
	}

	@Override
	public boolean deleteSlice(String sliceName) throws SliceNotFound, PermissionDeniedException {
		String changerSlice = APIUserCred.getUserName();
		if(!APIAuth.transitivelyCreated(changerSlice,sliceName))
			throw new PermissionDeniedException("Slice " + changerSlice + 
					" does not have perms to change the passwd of " + sliceName); 		
		try {
			FVConfig.deleteSlice(sliceName);
		} catch (Exception e) {
			throw new SliceNotFound("slice does not exist: " + sliceName);
		}
		return true;
	}

	/**
	 * Implements {@link org.flowvisor.api.FVUserAPI#changeFlowSpace}
	 * 
	 * Allow this change if it affectst the flowspace delagated to this
	 * slice.  
	 * 
	 */
	
	@Override
	public boolean changeFlowSpace(List<Map<String,String>> changes) throws MalformedFlowChange{
		// FIXME: implement security for who can change what
		String user = APIUserCred.getUserName();
		FlowMap flowSpace = FVConfig.getFlowSpaceFlowMap();
		for(int i=0; i< changes.size(); i++) {
			FlowChange change = FlowChange.fromMap(changes.get(i)); 
			FVLog.log(LogLevel.INFO, null, "user " + user + " " + change.getOperation() +
					" at index=" + change.getIndex() + " for dpid=" + 
					HexString.toHexString(change.getDpid()) + " match=" +
					change.getMatch() + " actions=" + 
					FlowSpaceUtil.toString(change.getActions())
					);
			switch(change.getOperation()) {
			case ADD:
				flowSpace.addRule(change.getIndex(), 
						new FlowEntry(
								change.getDpid(),
								change.getMatch(),
								change.getActions()
								));
				break;
			case REMOVE:
				flowSpace.getRules().remove(change.getIndex());
				break;
			case CHANGE:
				flowSpace.getRules().set(change.getIndex(),new FlowEntry(
						change.getDpid(),
						change.getMatch(),
						change.getActions()
						)); 
				break;
			default:
				FVLog.log(LogLevel.WARN, null, "user " + user + 
						" ignoring unknown changeFlow event: " + 
						change.getOperation());
			}
		}
		// update the indexes at the end, not with each rule
		FlowSpaceUtil.updateFlowSpaceIndexes();
		return true;
	}

	@Override
	public String[] listSlices() throws PermissionDeniedException {
		if(!FVConfig.isSupervisor(APIUserCred.getUserName()))
			throw new PermissionDeniedException("listSlices only available to root");
		String[] sliceArr=null;
		try {
			List<String> slices = FVConfig.list(FVConfig.SLICES);
			sliceArr = new String[slices.size()];
			slices.toArray(sliceArr);
		} catch (ConfigError e) {
			e.printStackTrace();
			new RuntimeException("wtf!?: no SLICES subdir found in config");
		}
		return sliceArr;
	}

	@Override
	public Map<String, String> getSliceInfo(String sliceName) throws PermissionDeniedException {
		HashMap<String,String> map = new HashMap<String,String>();
		String user = APIUserCred.getUserName();
		if(!FVConfig.isSupervisor(user)&&
				!APIAuth.transitivelyCreated(user, sliceName))
			throw new PermissionDeniedException("not superuser or transitive slice creator");
		String base = FVConfig.SLICES + "." + sliceName + ".";
		
		try {
			map.put("contact_email", FVConfig.getString(base + "contact_email"));
			map.put("controller_hostname", FVConfig.getString(base + "controller_hostname"));
			map.put("controller_port", String.valueOf(FVConfig.getInt(base+ "controller_port")));
			map.put("creator", FVConfig.getString(base + "creator"));
		} catch (ConfigError e) {
			FVLog.log(LogLevel.CRIT, null, "malformed slice: " + e);
			e.printStackTrace();
		}
		
		return map;
	}
	
	

}
