package org.flowvisor.api;

import java.util.List;
import java.util.Map;

import org.flowvisor.config.ConfigError;
import org.flowvisor.exceptions.DPIDNotFound;
import org.flowvisor.exceptions.MalformedControllerURL;
import org.flowvisor.exceptions.MalformedFlowChange;
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
	public boolean change_password(String sliceName, String newPasswd) throws PermissionDeniedException;
	
	/**
	 * Get the list of device DPIDs (e.g., switches, routers, APs) connected to the FV
	 * 
	 * @return
	 */
	public List<String> listDevices();
	
	/**
	 * Get information about a device
	 * 
	 * @param dpidStr  8 colon separated hex bytes, e..g., "00:00:00:00:00:00:00:01"
	 * 
	 * @return a map of key=value pairs where the value may itself be a more complex object
	 */
	public Map<String,String> getDeviceInfo(String dpidStr) throws DPIDNotFound;
	
	/**
	 * Get the list of links between the devices in getDevices()
	 * Links are directional, so switch1 --> switch2 does not imply
	 * the reverse; they will be both listed if the link is bidirectional
	 * @return
	 */
	
	public List<Map<String,String>> getLinks();
	
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
	 * FIXME: make this more codified; is XMLRPC the right thing here? Protobufs?
	 * 
	 * Each Map should contain the following elements; all keys and values are strings
	 *    key="operation", value={CHANGE,ADD,REMOVE}
	 *    key="index",     value=index into flowspace for the operation
	 *    
	 *    additionally, ADD and CHANGE operations should contain
	 *    key="dpid", value=8 octet hexcoded string, e.g., "00:00:23:20:10:25:55:af"
	 *    key="match", value=dpctl-style OFMatch string, see below
	 *    key="actions", value=comma separated string of SliceActions suitable to call SliceAction.fromString
	 *    	e.g., "SliceAction:alice=4,SliceAction:bob=2
	 *    
	 *    FIXME: change perms flags to human readable letters, e.g., "(r)read,(w)rite,(d)elegate"
	 *    
	 *   The "match" value string is a comma separated string of the form "match_field=value", e.g.,
	 *   "in_port=5,dl_src=00:43:af:35:22:11,tp_src=80" similar to dpctl from the OpenFlow reference switch.
	 *   Any field not explicitly listed is assumed to be wildcarded.
	 *     
	 *   The string will get wrapped with "OFMatch[" + match_value + "]" and passed off to 
	 *   OFMatch.fromString("OFMatch[" + match_value + "]") and generally follows the same convention
	 *   as dpctl 
	 * 
	 * @param list of changes
	 * @throws MalformedFlowChange
	 */
	public boolean changeFlowSpace(List<Map<String,String>> changes) throws MalformedFlowChange;
	
	/**
	 * Return a list of slices in the flowvisor: 
	 * root only!
	 * @return
	 */
	public List<String> listSlices() throws PermissionDeniedException;
	
	
	/**
	 * Returns a list of strings that represents the requested config element
	 * @param nodeName config element name
	 * @return List of strings
	 * @throws ConfigError
	 */
	public List<String> getConfig(String nodeName) throws ConfigError, PermissionDeniedException; 
	
	/**
	 * Sets a config element by name
	 * @param nodeName config element name
	 * @param value string representation of value
	 * @return success
	 * @throws ConfigError
	 */
	public boolean setConfig(String nodeName, String value) throws ConfigError, PermissionDeniedException;
	
	/**
	 * Reload last checkpointed config from disk
	 * 
	 * Only available to root
	 * 
	 * TODO: implement!
	 * @return success
	 */
	public boolean revertToLastCheckpoint();
	
}
