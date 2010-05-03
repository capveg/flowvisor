/**
 * 
 */
package org.flowvisor.api;

import java.util.ArrayList;
import java.util.List;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.APIUserCred;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.exceptions.MalformedControllerURL;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.exceptions.SliceNotFound;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;

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
		if (sliceName.equals(FVConfig.getRoot()))
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
	public void changePasswd(String sliceName, String newPasswd) throws PermissionDeniedException {
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
		
	}

	@Override
	public void change_passwd(String sliceName, String newPasswd)
			throws PermissionDeniedException {
		changePasswd(sliceName, newPasswd);
		// just call changePasswd(); keeping the two names made things easier for Jad /shrug
	}

	/**
	 * For now, create a circular, bidirectional loop between existing switches
	 * FIXME need to actually infer and calc real topology
	 */
	
	@Override
	public LinkAdvertisement[] getLinks() {
		DeviceAdvertisement[] devices = getDevices();
		LinkAdvertisement[] list = new LinkAdvertisement[devices.length*2];
		int linkIndex;
		for(int i=0;i<devices.length; i++) {
			// forward direction
			linkIndex=i*2;
			list[linkIndex].srcDPID = devices[i].dpid;
			list[linkIndex].dstDPID = devices[(i+1)%devices.length].dpid;
			list[linkIndex].srcPort = 0;
			list[linkIndex].dstPort = 1;
			list[linkIndex].attributes.put("fakeLink", "true");
			// reverse direction
			linkIndex=i*2+1;
			list[linkIndex].dstDPID = devices[i].dpid;
			list[linkIndex].srcDPID = devices[(i+1)%devices.length].dpid;
			list[linkIndex].dstPort = 0;
			list[linkIndex].srcPort = 1;
			list[linkIndex].attributes.put("fakeLink", "true");
		}
		return list;
	}
	
	@Override
	public DeviceAdvertisement[] getDevices(){
		FlowVisor fv = FlowVisor.getInstance();
		// get list from main flowvisor instance
		List<FVClassifier> classifiers = new ArrayList<FVClassifier>();
		for(FVEventHandler handler : fv.getHandlers()) {
			if(handler instanceof FVClassifier)
				classifiers.add((FVClassifier) handler);
		}
		DeviceAdvertisement[] list = new DeviceAdvertisement[classifiers.size()];
		for(int i=0; i < classifiers.size(); i++) {
			FVClassifier classifier = classifiers.get(i);
			list[i].dpid = classifier.getSwitchInfo().getDatapathId();
			// TODO get and cache STATS_DESC info
			list[i].hw_desc = "unimplemented";
			list[i].mfr_desc = "unimplemented";
			list[i].serial_num = "unimplemented";
			list[i].dp_desc = "unimplemented";
			list[i].attributes.put("nPorts", 
					String.valueOf(classifier.getSwitchInfo().getPorts().size()));
		}
		return list;
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
	public void changeFlowSpace(FlowChange[] changes) {
		// TODO Auto-generated method stub
		
	}

}
