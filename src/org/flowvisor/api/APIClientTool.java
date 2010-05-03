/**
 * 
 */
package org.flowvisor.api;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.flowvisor.config.FVConfig;

/**
 * Client side stand alone command-line tool for invoking the FVUserAPI
 *   
 * This is pretty hacky and just for testing; people should write their
 * own clients and/or call the XMLRPC directly  
 * @author capveg
 *
 */
public class APIClientTool {
	String URL;
	XmlRpcClientConfigImpl config;
	XmlRpcClient client;
	static APICmd[] cmdlist = new APICmd[] { 
		new APICmd("ping",1),
		new APICmd("listFlowSpace",0),
		new APICmd("deleteSlice",1),
		new APICmd("changePasswd",1),
		new APICmd("getDevices",0),
		new APICmd("getLinks",0),
		new APICmd("listSlices",0),
		new APICmd("createSlice",3)
	};
	static class APICmd {
		String name;
		int argCount;
		static HashMap<String,APICmd> cmdlist = new HashMap<String,APICmd>();
		APICmd(String name, int argCount) {
			this.name = name;
			this.argCount = argCount;
			cmdlist.put(name, this);
		}
		@SuppressWarnings("unchecked")  // Need to figure out magic java sauce to fix this
		void invoke(APIClientTool client, String args[]) 
		throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			Class<String>[] params = new Class[args.length];
			for(int i=0; i< args.length; i++)
				params[i]= String.class;
			Method m = APIClientTool.class.getMethod("run_" + 
					this.name, params);
			m.invoke(client, (Object[])args);
		}
	}

	/**
	 * 
	 * @param URL Server URL
	 */
	public APIClientTool(String URL) {
		this.URL = URL;
	}

	/**
	 * Init connection to XMLRPC Server in URL
	 * @throws MalformedURLException 
	 * 
	 * @throws Exception
	 */
	public void init(String user, String passwd) throws MalformedURLException {
		this.installDumbTrust();
		config = new XmlRpcClientConfigImpl();
		config.setBasicUserName(user);
		config.setBasicPassword(passwd);
		config.setServerURL(new URL(this.URL));
	    
		client = new XmlRpcClient();
		//client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
		//client.setTransportFactory(new )
		client.setConfig(config);
	}
	
	public void installDumbTrust() {

	    // Create a trust manager that does not validate certificate chains
	    TrustManager[] trustAllCerts = new TrustManager[] {
	        new X509TrustManager() {
	            public X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	 
	            public void checkClientTrusted(X509Certificate[] certs, String authType) {
	                // Trust always
	            }
	 
	            public void checkServerTrusted(X509Certificate[] certs, String authType) {
	                // Trust always
	            }
	        }
	    };
	    try {
	    	// Install the all-trusting trust manager
	    	SSLContext sc = SSLContext.getInstance("SSL");
	    	// Create empty HostnameVerifier
	    	HostnameVerifier hv = new HostnameVerifier() {
	    		public boolean verify(String arg0, SSLSession arg1) {
	    			return true;
	    		}
	    	};


	    	sc.init(null, trustAllCerts, new java.security.SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		    HttpsURLConnection.setDefaultHostnameVerifier(hv);
	    } catch (KeyManagementException e) {
	    	e.printStackTrace();
	    	throw new RuntimeException(e);
	    } catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
	    }

	}
	
	public void run_getDevices() throws XmlRpcException {
		Object[] reply = (Object[]) this.client.execute("api.getDevices", new Object[] {});
		if(reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		for(int i=0;i < reply.length; i++) {
			DeviceAdvertisement ad = (DeviceAdvertisement) reply[i];
			System.out.println("Device "+i+": " + ad);
		}
	}
	
	public void run_getLinks() throws XmlRpcException {
		Object[] reply = (Object[]) this.client.execute("api.getLinks", new Object[] {});
		if(reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		for(int i=0;i < reply.length; i++) {
			LinkAdvertisement ad = (LinkAdvertisement) reply[i];
			System.out.println("Device "+i+": " + ad);
		}
	}
	
	public void run_changePasswd(String sliceName) throws IOException, XmlRpcException {
		String passwd = FVConfig.readPasswd("New password: ");
		Boolean reply = (Boolean) this.client.execute("api.changePasswd", 
						new Object[] {sliceName,passwd});
		if(reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply) 
			System.err.println("success!");
		else 
			System.err.println("failed!");
	}

	public void run_createSlice(String sliceName, String controller_url, String slice_email) throws IOException, XmlRpcException {
		String passwd = FVConfig.readPasswd("New password: ");
		Boolean reply = (Boolean) this.client.execute("api.createSlice", 
						new Object[] {sliceName,passwd,controller_url, slice_email});
		if(reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		if (reply) 
			System.err.println("success!");
		else 
			System.err.println("failed!");
	}

	
	public void run_ping(String msg) throws XmlRpcException {
		String reply = (String) this.client.execute("api.ping", new Object[] { msg });
		if(reply != null) {
			System.err.println("Got reply:");
			System.out.println(reply);
		}
		else {
			System.err.println("Got 'null' for reply :-(");
		}
	}

	public void run_deleteSlice(String sliceName) throws XmlRpcException {
		Boolean reply = (Boolean) this.client.execute("api.deleteSlice", new Object[] { sliceName });
		if(reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}			
		if (reply) 
			System.err.println("success!");
		else 
			System.err.println("failed!");	
	}
	
	public void run_listSlices() throws XmlRpcException {
		Object[] reply = (Object[]) this.client.execute("api.listSlices", new Object[] {});
		if(reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		for(int i=0;i < reply.length; i++) {
			String slice = (String) reply[i];
			System.out.println("Slice "+i+": " + slice);
		}
		
	}

	
	public void run_listFlowSpace() throws XmlRpcException {
		Object[] result2 = (Object[]) client.execute("api.listFlowSpace", new Object[]{});
		if(result2 != null) {
			System.err.println("Got reply:");
			int i;
			for (i=0; i< result2.length; i++)
				System.err.println("rule " + i + ": " + (String)result2[i]);
		}	else {
			System.err.println("Got 'null' for reply :-(");
		}
	}
	
	/**
	 * Front-end cmdline parser for APIClientTool
	 * 
	 * @param args
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws MalformedURLException 
	 */
	public static void main(String args[]) 
		throws SecurityException, 
			IllegalArgumentException, 
			NoSuchMethodException, 
			IllegalAccessException, 
			InvocationTargetException, MalformedURLException {
		// FIXME: make URL a parameter
		//APIClientTool client = new APIClientTool("https://root:joemama@localhost:8080/xmlrpc");
		APIClientTool client = new APIClientTool("https://localhost:8080/xmlrpc");
		client.init("root","0fw0rk");
		if(args.length == 0)
			usage("need to specify a command");
		APICmd cmd = APICmd.cmdlist.get(args[0]);
		if(cmd == null)
			usage("command '" + args[0] + "' does not exist");
		if ( (args.length -1)  < cmd.argCount)
			usage("command '" + args[0] + "' takes " + cmd.argCount + 
					" args: only " + (args.length -1) + " given");
		String[] strippedArgs = new String[args.length-1];
		System.arraycopy(args, 1, strippedArgs, 0, strippedArgs.length);
		cmd.invoke(client, strippedArgs);
	}

	private static void usage(String string) {
		System.err.println(string);
		System.err.println("Usage: APIClientTool: ");
		for(int i=0; i< APIClientTool.cmdlist.length; i++) {
			APICmd cmd = APIClientTool.cmdlist[i];
			System.err.println("\t" + cmd.name + " takes " + 
					cmd.argCount + " args");
		}
		System.exit(-1);
	}
}
