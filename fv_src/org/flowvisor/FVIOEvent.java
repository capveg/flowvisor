package org.flowvisor;

import java.nio.channels.*;
import org.flowvisor.*;

class FVIOEvent extends FVEvent
{
    int op;
    SelectorKey sk;

    public FVIOEvent(FVMod src, int type, int code, int op,SelectorKey sk)
    {
        super(src,type,code);
        this.op=op;
        this.sk = sk;
    }

    public SelectorKey getSelectorKey() { return this.sk; }
    public int getOp() { return this.op; } 
}
