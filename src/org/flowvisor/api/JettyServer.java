package org.flowvisor.api;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lib.jsonrpc.RPCService;

import org.eclipse.jetty.http.security.Constraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;

public class JettyServer implements Runnable{

	public static final int default_jetty_port = 8081;

	public static String REALM_NAME = "JETTYREALM";
	private Server jettyServer;

	protected RPCService service = new FVUserAPIImpl();

	public JettyServer(){
		init();
	}

	private void init(){
		int port;

		try {
			port = FVConfig.getInt(FVConfig.API_JETTY_WEBSERVER_PORT);
		} catch (ConfigError e) {
			port = default_jetty_port; // not explicitly configured
		}

		jettyServer = new Server(port);
		Connector connector = new SelectChannelConnector();
		connector.setPort(port);

		SslSelectChannelConnector sslConnector = new SslSelectChannelConnector();

		sslConnector.setPort(8443);
		String sslKeyStore = System.getProperty("javax.net.ssl.keyStore");
		if (sslKeyStore == null) {
			throw new RuntimeException(
			"Property javax.net.ssl.keyStore not defined; are you correctly using the flowvisor wrapper script?");
		}
		if (!(new File(sslKeyStore)).exists())
			throw new RuntimeException("SSL Key Store file not found: '"
					+ sslKeyStore
					+ "'\nPlease generate with `fvconfig generateCert`");
		sslConnector.setKeystore(sslKeyStore);

		String sslKeyStorePW = System.getProperty("javax.net.ssl.keyStorePassword");
		sslConnector.setPassword(sslKeyStorePW);

		jettyServer.addConnector(sslConnector);


		jettyServer.setConnectors(new Connector[]{sslConnector});



		// Set up context
		/*ContextHandler context = new ContextHandler();
		context.setContextPath("/flowvisor");
		context.setResourceBase(".");
		context.setClassLoader(Thread.currentThread().getContextClassLoader());
		context.setAllowNullPathInfo(true);
		context.setServer(jettyServer);*/

		// Set up Security
		ConstraintSecurityHandler authHandler = createAuthenticationHandler(jettyServer);
		authHandler.setHandler(new AuthenticationHandler());
		//context.setHandler(authHandler);
	}

	@Override
	public void run(){

		try {
			jettyServer.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			jettyServer.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}

	public class AuthenticationHandler extends AbstractHandler{

		@Override
		public final void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
		throws IOException, ServletException
		{
			if(baseRequest.getAuthentication().equals(Authentication.UNAUTHENTICATED)){
				response.sendError(Response.SC_UNAUTHORIZED, "");
				baseRequest.setHandled(true);
				return;
			}

			service.dispatch(request, response);
			baseRequest.setHandled(true);
		}
	}

	private ConstraintSecurityHandler createAuthenticationHandler(Server server){
		ConstraintSecurityHandler security = new ConstraintSecurityHandler();
		security.setRealmName(REALM_NAME);
		server.setHandler(security);

		// Not currently using constraints for rules, but perhaps in the future? Here's an example if so...
		Constraint constraint = new Constraint();
		constraint.setName("auth");
		constraint.setAuthenticate( true );
		constraint.setRoles(new String[]{"user", "admin"});


		ConstraintMapping mapping = new ConstraintMapping();
		mapping.setPathSpec( "/*" );
		mapping.setConstraint( constraint );
		Set<String> knownRoles = new HashSet<String>();
		knownRoles.add("user");
		knownRoles.add("admin");
		security.setConstraintMappings(new ConstraintMapping[] {mapping}, knownRoles);
		security.setAuthenticator(new FlowVisorAuthenticator());

		LoginService loginService = new HashLoginService(REALM_NAME);
		server.addBean(loginService);
		security.setLoginService(loginService);
		security.setStrict(false);

		return security;
	}
}