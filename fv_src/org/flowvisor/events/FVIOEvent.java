package org.flowvisor.events;

import java.nio.channels.*;
import org.flowvisor.*;

public class FVIOEvent extends FVEvent
{
    SelectionKey sk;
    /****
     * Signal to an FVMod that someIO they've selected() for is ready
     * @param src The source 
     * @param sk
     */
    public FVIOEvent(FVMod src,SelectionKey sk)
    {
        super(src,FVET_IO,sk.readyOps());  
        this.sk = sk;
    }

    public SelectionKey getSelectionKey() { return this.sk; }
}
