
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
	
	// VERSION
	public final static String FLOVISOR_VERSION = "flowvisor-0.6-alpha";
	
	// Max slicename len ; used in LLDP for now; needs to be 1 byte 
	public final static int MAX_SLICENAME_LEN = 255;
		
    public static void main(String args[]) throws IOException,UnhandledEvent,ConfigError
    {
    	ArrayList<FVEventHandler> handlers = new ArrayList<FVEventHandler>();

    	// FIXME :: do real arg parsing
    	if (args.length == 0 )
    		usage("need to specify config");
    	
    	// load  config from file
    	FVConfig.readFromFile(args[0]);
    	
    	// init polling loop
    	FVEventLoop pollLoop = new FVEventLoop();
    	  	
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

    /**
     * Print usage message and warning string then exit
     * @param string warning
     */
    
	private static void usage(String string) {
		System.err.println("err: " + string );
		System.err.println("Usage: FlowVisor configfile.xml");
		System.exit(-1);
	}
}
