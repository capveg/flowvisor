package org.flowvisor.api;

import java.io.IOException;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * This is stolen pretty directly from the apache-xml example code.
 * 
 * FIXME: Come back and make asynchronous FIXME: address all of the issues with
 * the WebServer code that the author's bring up
 * 
 * @author capveg
 * 
 */

public class APIServer {

	// FIXME: replace with a FVConfig entry
	private static final int default_port = 8080;

	public static int getDefaultPort() {
		return default_port;
	}

	/**
	 * Spawn a thread to run the XMLRPC FlowVisor UserAPI WebServer
	 * 
	 * @return the webServer
	 * @throws XmlRpcException
	 * @throws IOException
	 * @throws Exception
	 */
	public static WebServer spawn() throws XmlRpcException, IOException {
		int port;

		try {
			port = FVConfig.getInt(FVConfig.API_WEBSERVER_PORT);
		} catch (ConfigError e) {
			port = default_port; // not explicitly configured
		}

		WebServer webServer = new SSLWebServer(port);

		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

		PropertyHandlerMapping phm = new PropertyHandlerMapping();

		phm.addHandler("api", org.flowvisor.api.FVUserAPIImpl.class);
		phm.setAuthenticationHandler(new APIAuth());
		xmlRpcServer.setHandlerMapping(phm);

		XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer
				.getConfig();
		// Unset this for now, for python folks:
		// http://bugs.python.org/issue8792
		// XMLRPC is stupid -- need to replace
		// serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);
		FVLog.log(LogLevel.INFO, null,
				"initializing FlowVisor UserAPI XMLRPC SSL WebServer on port "
						+ port);
		webServer.start();
		return webServer;
	}

}
