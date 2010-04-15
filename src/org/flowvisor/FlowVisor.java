
package org.flowvisor;

import java.io.*;
import java.util.ArrayList;
import org.flowvisor.exceptions.*;
import org.flowvisor.ofswitch.OFSwitchAcceptor;
import org.flowvisor.events.*;
import org.flowvisor.config.*;

public class FlowVisor
{
	// VENDOR EXTENSION ID
	public final static int FLOWVISOR_VENDOR_EXTENSION = 0x80000001;
	public final static String FLOVISOR_VERSION = "flowvisor-0.6-alpha";
	
    public static void main(String args[]) throws IOException,UnhandledEvent,ConfigError
    {
    	ArrayList<FVEventHandler> handlers = new ArrayList<FVEventHandler>();

    	// init default config
    	DefaultConfig.init();
    	
    	// init polling loop
    	FVEventLoop pollLoop = new FVEventLoop();
    	
    	if(args.length > 0) // FIXME: do real arg parsing
    		FVConfig.setInt(FVConfig.LISTEN_PORT, Integer.valueOf(args[0]));
  	
    	int port = FVConfig.getInt(FVConfig.LISTEN_PORT);
    	
    	// init switchAcceptor
    	OFSwitchAcceptor acceptor	= new OFSwitchAcceptor(
    										"ofswitchAcceptor",
    										pollLoop, 
    										port, 
    										16);
    	handlers.add(acceptor);				
    	
    	// start event processing
    	pollLoop.doEventLoop();
    	
    	/** 
    	 * FIXME add a cleanup call to event handlers
    	// now shut everything down
    	for (FVEventHandler fvh : handlers)
    		fvh.cleanup();
    	*/
    }
}
