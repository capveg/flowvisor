package org.flowvisor.events;

import java.nio.channels.*;
import org.flowvisor.*;

public class FVIOEvent extends FVEvent
{
    int op;
    SelectionKey sk;

    public FVIOEvent(FVMod src, int type, int code, int op,SelectionKey sk)
    {
        super(src,type,code);
        this.op=op;
        this.sk = sk;
    }

    public SelectionKey getSelectionKey() { return this.sk; }
    public int getOp() { return this.op; } 
}
