/**
 * 
 */
package org.flowvisor.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

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
		new APICmd("listFlowSpace",0)
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
	public void init() throws MalformedURLException {
		config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(this.URL));
		client = new XmlRpcClient();
		client.setConfig(config);
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
		APIClientTool client = new APIClientTool("http://127.0.0.1:8080/xmlrpc");
		client.init();
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
