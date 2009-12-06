package org.flowvisor;

import java.nio.*;
import java.nio.channels.*;

import org.flowvisor.*;

class FVSwitchAcceptor implements FVMod
{
    String name;
    FVPollLoop pollLoop;
    public FVAcceptor(String name, int port, int backlog, FVPollLoop pollLoop)
    {
        this.name = name;
        this.listenPort = port;
        this.backlog = backlog;
        this.pollLoop = pollLoop;

        ssc = ServerSocketChannel.open();
        ssc.socket().setReuseAddress(true);
        ssc.socket().bind(new InetSocketAddress(port), backlog);

        // register this module with the polling loop
        pollLoop.register(ssc, OP_ACCEPT, this)
    }

    public String getName() { return this.name; }
    public void handleEvent(FVEvent event)
    {
        switch(event.getType())
        {
            case FVET_IO:
                handleIOEvent((FVIOEvent) event);
                break;
            default:
                 throw new FVUnhandledEvent("don't know how to deal with event " + e);
        }
    }

    private void handleIOEvent(FVIOEvent event)
    {
        System.err.println("GOt a new swtich connection...ignoring");
    }
}
