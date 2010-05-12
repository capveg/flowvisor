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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.flowvisor.api.FlowChange.FlowChangeOp;
import org.flowvisor.config.FVConfig;
import org.flowvisor.exceptions.MalformedFlowChange;

/**
 * Client side stand alone command-line tool for invoking the FVUserAPI
 *   
 * This is pretty hacky and just for testing; people should write their
 * own clients and/or call the XMLRPC directly  
 * @author capveg
 *
 */
public class FVCtl {
	String URL;
	XmlRpcClientConfigImpl config;
	XmlRpcClient client;
	static APICmd[] cmdlist = new APICmd[] { 
		new APICmd("listFlowSpace",		0	),
		new APICmd("getDevices",		0	),
		new APICmd("getLinks",			0	),
		new APICmd("listSlices",		0	),
		new APICmd("ping",				1, "<msg>"),
		new APICmd("deleteSlice",		1, "<slicename>"),
		new APICmd("changePasswd",		1, "<slicename>"),
		new APICmd("getSliceInfo", 		1, "<slicename>"),
		new APICmd("getDeviceInfo",     1, "<dpid>"),
		new APICmd("createSlice",		3, "<slicename> <controller_url> <email>"),
		new APICmd("removeFlowSpace", 	1, "<index>"),
		new APICmd("addFlowSpace", 		4, "<index> <dpid> <match> <actions>"),
		new APICmd("changeFlowSpace", 	4, "<index> <dpid> <match> <actions>")
	};
	static class APICmd {
		String name;
		int argCount;
		String usage;
		static HashMap<String,APICmd> cmdlist = new HashMap<String,APICmd>();
		APICmd(String name, int argCount, String usage) {
			this.name = name;
			this.argCount = argCount;
			this.usage=usage;
			cmdlist.put(name, this);
		}
		APICmd(String name, int argCount) {
			this(name,argCount,"");
		}
		@SuppressWarnings("unchecked")  // Need to figure out magic java sauce to fix this
		void invoke(FVCtl client, String args[]) 
		throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			Class<String>[] params = new Class[args.length];
			for(int i=0; i< args.length; i++)
				params[i]= String.class;
			Method m = FVCtl.class.getMethod("run_" + 
					this.name, params);
			m.invoke(client, (Object[])args);
		}
	}

	/**
	 * 
	 * @param URL Server URL
	 */
	public FVCtl(String URL) {
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
		config.setEnabledForExtensions(true);
	    
		client = new XmlRpcClient();
		//client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
		//client.setTransportFactory(new )
		client.setConfig(config);
	}
	
	public void installDumbTrust() {

	    // Create a trust manager that does not validate certificate chains
	    System.err.println("WARN: blindly trusting server cert - FIXME");
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
			String dpid = (String) reply[i];
			System.out.println("Device "+i+": " + dpid);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void run_getDeviceInfo(String dpidStr) throws XmlRpcException {
		Map<String,Object> reply = (Map<String,Object>) this.client.execute("api.getDeviceInfo", new Object[] {dpidStr});
		if(reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		for(String key: reply.keySet()) {
			System.out.println(key+"="+reply.get(key));
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

	@SuppressWarnings("unchecked")
	public void run_getSliceInfo(String sliceName) throws IOException, XmlRpcException {
		
		Object o =  this.client.execute("api.getSliceInfo", 
				new Object[] {sliceName});
		if(o == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}
		Map<String,String> reply = null;
		if (o instanceof Map<?, ?>)
			reply = (Map<String,String>) o; 
			
		System.err.println("Got reply:");
		for(String key: reply.keySet()) 
			System.out.println(key + "=" + reply.get(key));
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

	public void run_removeFlowSpace(String indexStr) throws XmlRpcException {
		FlowChange change = new FlowChange(FlowChangeOp.REMOVE, Integer.valueOf(indexStr));
		List<Map<String,String>> mapList = new LinkedList<Map<String,String>>();
		mapList.add(change.toMap());
		Boolean reply = (Boolean) this.client.execute("api.changeFlowSpace", 
				new Object[] { mapList  });
					
		if(reply == null) {
			System.err.println("Got 'null' for reply :-(");
			System.exit(-1);
		}			
		if (reply) 
			System.err.println("success!");
		else 
			System.err.println("failed!");	
	}

	public void run_addFlowSpace(String indexStr, String dpid, String match, String actions) 
					throws XmlRpcException, MalformedFlowChange {
		do_flowSpaceChange(FlowChangeOp.ADD, indexStr, dpid, match, actions);
	}

	public void run_changeFlowSpace(String indexStr, String dpid, String match, String actions) 
		throws XmlRpcException, MalformedFlowChange {
		do_flowSpaceChange(FlowChangeOp.CHANGE, indexStr, dpid, match, actions);
	}

	private void do_flowSpaceChange(FlowChangeOp op, String indexStr, String dpid, 
			String match, String actions) throws MalformedFlowChange, XmlRpcException{
	Map<String,String> map = FlowChange.makeMap(op, indexStr, dpid, match,actions);
	System.err.print("Local sanity checks: ");
	FlowChange.fromMap(map);	// could throw MalformedFlowChange
	System.err.print("passed");
	List<Map<String,String>> mapList = new LinkedList<Map<String,String>>();
	mapList.add(map);
	Boolean reply = (Boolean) this.client.execute("api.changeFlowSpace", new Object[] { mapList });
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
	 * Front-end cmdline parser for FVCtl
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
		//FVCtl client = new FVCtl("https://root:joemama@localhost:8080/xmlrpc");
		FVCtl client = new FVCtl("https://localhost:8080/xmlrpc");
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
		System.err.println("Usage: FVCtl: ");
		for(int i=0; i< FVCtl.cmdlist.length; i++) {
			APICmd cmd = FVCtl.cmdlist[i];
			System.err.println("\t" + cmd.name + " " + 
					cmd.usage);
		}
		System.exit(-1);
	}
}
