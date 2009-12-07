
package org.flowvisor;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import org.flowvisor.exceptions.*;
import org.flowvisor.ofswitch.OFSwitchAcceptor;

class Flowvisor
{
    public static void main(String args[]) throws IOException,UnhandledEvent
    {
    	ArrayList<FVMod> fvmods = new ArrayList<FVMod>();

    	// init polling loop
    	FVPollLoop pollLoop			= new FVPollLoop();
    	fvmods.add(pollLoop);	// collect these up for now...
    	
    	// init switchAcceptor
    	OFSwitchAcceptor acceptor	= new OFSwitchAcceptor(
    										"ofswitchAcceptor",
    										pollLoop, 
    										6633, 
    										16);
    	fvmods.add(acceptor);
    	
    	// start event processing
    	pollLoop.doPollLoop();
    	
    	// now shut everything down
    	for(Iterator<FVMod> i = fvmods.iterator(); i.hasNext(); i.next())
    		i.next().cleanup();
    }
}
