package org.flowvisor;

import java.nio.*;
import org.flowvisor.*;

interface FVMod implements Runnable
{
    void handleEvent(FVEvent event);
    String getName();
}
