package org.flowvisor.api;

import java.util.Map;

import org.flowvisor.exceptions.MalformedControllerURL;
import org.flowvisor.exceptions.PermissionDeniedException;
import org.flowvisor.exceptions.SliceNotFound;

public interface FVUserAPI {

	/** 
	 * For debugging
	 * @param arg test string
	 * @return response test string
	 */
	public String ping(String arg);
	
	/**
	 * Lists all the flowspace this user has control over
	 * 
	 * @return
	 */
	public String[] listFlowSpace();
	
	/**
	 * Create a new slice (without flowspace)
	 * 
	 * @param sliceName
	 * @param passwd Cleartext! FIXME
	 * @param controller_url Reference controller pseudo-url, e.g., tcp:hostname[:port]
	 * @param slice_email As a contract for the slice
	 * @return success
	 */
	
	public boolean createSlice(String sliceName, String passwd, 
			String controller_url, String slice_email) throws MalformedControllerURL;
	
	public Map<String,String> getSliceInfo(String sliceName) throws PermissionDeniedException;
	
	/**
	 * Change the password for this slice
	 * 
	 * A slice is allowed to change its own password and the password
	 * of any slice that it has (transitively) created
	 * 
	 * @param sliceName
	 * @param newPasswd
	 */
	public boolean changePasswd(String sliceName, String newPasswd) throws PermissionDeniedException;
	// have both names, b/c it makes the OM's life easier
	public boolean change_passwd(String sliceName, String newPasswd) throws PermissionDeniedException;
	
	/**
	 * Get the list of devices (e.g., switches, routers, APs) connected to the FV
	 * 
	 * @return
	 */
	public DeviceAdvertisement[] getDevices();
	
	
	/**
	 * Get the list of links between the devices in getDevices()
	 * Links are directional, so switch1 --> switch2 does not imply
	 * the reverse; they will be both listed if the link is bidirectional
	 * @return
	 */
	
	public LinkAdvertisement[] getLinks();
	
	/**
	 * Delete the named slice
	 * 
	 * Requestor only has permission to delete its own slice or the slice that it 
	 * (transitively) created.  Since root has transitively created all slices, root
	 * can delete all slices. 
	 * 
	 * @param sliceName
	 * @return  Success
	 * @throws {@link SliceNotFound}, {@link PermissionDeniedException}
	 */
	
	public boolean deleteSlice(String sliceName) throws SliceNotFound, PermissionDeniedException;

	
	/**
	 * Make changes to the flowspace
	 * 
	 * Changes are processed in order and only after the last change is 
	 * applied to the changes take affect, i.e., this is transactional
	 * 
	 * FIXME: create a test case to make sure it is in fact transactional
	 * 
	 * @param changes list of changes
	 */
	public boolean changeFlowSpace(FlowChange[] changes);
	
	/**
	 * Return a list of slices in the flowvisor: 
	 * root only!
	 * @return
	 */
	public String[] listSlices() throws PermissionDeniedException;
		
	
}
