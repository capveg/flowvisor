package org.flowvisor;

import java.net.*;
import java.io.*;
import java.nio.channels.*;
import org.flowvisor.events.FVIOEvent;


public class FVSwitchAcceptor extends FVMod
{
    String name;
    FVPollLoop pollLoop;
    int backlog;
    int listenPort;
    ServerSocketChannel ssc;
    public FVSwitchAcceptor(String name, FVPollLoop pollLoop, int port, int backlog) throws IOException
    {
        super(name,pollLoop);
        this.listenPort = port;
        this.backlog = backlog;
       
        
        ssc = ServerSocketChannel.open();
        ssc.socket().setReuseAddress(true);
        ssc.socket().bind(new InetSocketAddress(port), backlog);

        // register this module with the polling loop
        pollLoop.register(ssc, SelectionKey.OP_ACCEPT, this);
    }

    public void handleIOEvent(FVIOEvent event)
    {
        System.err.println("GOt a new swtich connection...ignoring");
    }
}
