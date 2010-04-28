/**
 * 
 */
package org.flowvisor.api;

import org.flowvisor.config.FVConfig;
import org.flowvisor.flows.FlowMap;

/**
 * This is the actual UserAPI that gets wrapped via XMLRPC
 * In theory ("God willin' and the creek dun rise"), XMLRPC
 * calls will call these function directly
 * 
 * @author capveg
 *
 */
public class FVUserAPI {
	/** 
	 * For debugging
	 * @param arg test string
	 * @return response test string
	 */
	public String ping(String arg) {
		return "PONG: " + arg;
	}
	public String[] listFlowSpace() {
		 
		FlowMap flowMap = FVConfig.getFlowSpaceFlowMap();
		String[] fs = new String[flowMap.countRules()];
		for(int i=0; i< fs.length; i++)
			fs[i] = flowMap.getRules().get(i).toString();
		return fs;
	}
}
