
package org.flowvisor;

import java.io.*;
import java.util.ArrayList;
import org.flowvisor.exceptions.*;
import org.flowvisor.ofswitch.OFSwitchAcceptor;
import org.flowvisor.events.*;
import org.flowvisor.config.*;

class FlowVisor
{
    public static void main(String args[]) throws IOException,UnhandledEvent,ConfigError
    {
    	ArrayList<FVEventHandler> handlers = new ArrayList<FVEventHandler>();

    	// init default config
    	DefaultConfig.init();
    	
    	// init polling loop
    	FVEventLoop pollLoop = new FVEventLoop();

    	// init switchAcceptor
    	OFSwitchAcceptor acceptor	= new OFSwitchAcceptor(
    										"ofswitchAcceptor",
    										pollLoop, 
    										FVConfig.getInt(DefaultConfig.LISTEN_PORT), 
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
