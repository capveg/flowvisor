/**
 * 
 */
package org.flowvisor.api;

import org.flowvisor.api.APIUserCred;
import org.flowvisor.config.FVConfig;
import org.flowvisor.exceptions.MalformedControllerURL;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.exceptions.SliceNotFound;
import org.flowvisor.flows.FlowMap;

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
		 
		FlowMap flowMap = FVConfig.getFlowSpaceFlowMap();
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
		changePasswd(sliceName, newPasswd); 
	}
	
	@Override
	public LinkAdvertisement[] getLinks() {
		LinkAdvertisement[] list = new LinkAdvertisement[0];
		return list;
	}
	
	@Override
	public DeviceAdvertisement[] getDevices(){
		DeviceAdvertisement[] list = new DeviceAdvertisement[0];
		return list;
	}
	
	@Override
	public boolean deleteSlice(String sliceName) throws SliceNotFound {
		// FIXME: make sure this user has permissions to do this operations
		
		try {
			FVConfig.deleteSlice(sliceName);
		} catch (Exception e) {
			throw new SliceNotFound("slice does not exist: " + sliceName);
		}
		return true;
	}

	@Override
	public void change_passwd(String sliceName, String newPasswd)
			throws PermissionDeniedException {
		// TODO Auto-generated method stub
		
	}



}
