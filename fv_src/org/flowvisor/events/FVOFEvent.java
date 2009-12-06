package org.flowvisor.events;

import org.flowvisor.*;
import org.flowvisor.ofswitch.OFSwitch;
import org.openflow.*;

public class FVOFEvent extends FVEvent
{
    OFMessage msg;
    OFSwitch sw;

    public FVOFEvent(FVMod src, int type, int code, OFMessage msg, OFSwitch sw)
    {
        super(src,type,code);
        this.msg=msg;
        this.sw = sw;
    }

    public OFSwitch getOFSwitch() { return this.sw; }
    public OFMessage getOFMessage() { return this.msg; } 
}
