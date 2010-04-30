/**
 * 
 */
package org.flowvisor.api;

import org.apache.xmlrpc.XmlRpcException;

import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfig;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping.AuthenticationHandler;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * Figure out if this request should be allowed or not
 * 
 * @author capveg
 *
 */
public class APIAuth implements AuthenticationHandler {

	/* (non-Javadoc)
	 * @see org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping.AuthenticationHandler#isAuthorized(org.apache.xmlrpc.XmlRpcRequest)
	 */
	@Override
	public boolean isAuthorized(XmlRpcRequest req) throws XmlRpcException {
		
		String method = req.getMethodName();
		XmlRpcHttpRequestConfig config =
            (XmlRpcHttpRequestConfig) req.getConfig();

		FVLog.log(LogLevel.DEBUG, null, "API call " + 
				method + " for user '" + config.getBasicUserName() + "'" );
		
		return true;
	}

}
