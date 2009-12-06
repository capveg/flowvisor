package org.flowvisor.events;

import org.flowvisor.FVMod;

public class FVEvent
{
    int type;
    int code;
    FVMod src;

    public final static int FVET_IO        = 0;
    public final static int FVET_OF_MSG    = 1;
    public final static int FVET_TIMER     = 2;

    public FVEvent(FVMod src, int type, int code)
    {
        this.src=src;
        this.type=type;
        this.code=code;
    }

    public FVMod getSrc() { return this.src; }
    public int getType() { return this.type; } 
    public int getCode() { return this.code; } 
}
