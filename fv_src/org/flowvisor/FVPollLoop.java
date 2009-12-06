package org.flowvisor;

import java.nio.*;
import java.io.*;

class FVPollLoop 
{

    Selector selector;

    public FVPollLoop()
    {
        selector = Selector.open();
    }


    public void register(SelectableChannel ch, int ops, FVMod mod)
    {
        ch.register(selector, ops, h);
    }

    public int addTimer(FVMod mod, FVTimerEvent t)
    {
    }

    public void doPollLoop()
    {
        // get time until next event
        // do select
        // if not timeout, dispatch events
    }
}
