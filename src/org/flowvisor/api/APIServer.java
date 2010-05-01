package org.flowvisor.api;

import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * This is stolen pretty directly from the apache-xml example code.
 * 
 * FIXME: Come back and make asynchronous
 * FIXME: address all of the issues with the WebServer code that the author's bring up 
 * @author capveg
 *
 */

public class APIServer {

	// FIXME: replace with a FVConfig entry
    private static final int port = 8080;

    /**
     * Spawn a thread to run the XMLRPC FlowVisor UserAPI WebServer
     * @return the webServer
     * @throws Exception
     */
    public static WebServer spawn() throws Exception {
        WebServer webServer = new SSLWebServer(port);

        XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

        PropertyHandlerMapping phm = new PropertyHandlerMapping();
       
        phm.addHandler("api", org.flowvisor.api.FVUserAPIImpl.class);
        phm.setAuthenticationHandler(new APIAuth());
        xmlRpcServer.setHandlerMapping(phm);

        XmlRpcServerConfigImpl serverConfig =
            (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
        serverConfig.setEnabledForExtensions(true);
        serverConfig.setContentLengthOptional(false);
        FVLog.log(LogLevel.INFO, null, "initializing FlowVisor UserAPI XMLRPC SSL WebServer");
        webServer.start();
        return webServer;
    }

	
}
