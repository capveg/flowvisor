package org.flowvisor.ofswitch;

import java.nio.channels.*;
import org.flowvisor.*;

/*****
 * This class handles events coming from and going to an OpenFlow switch.
 * 
 * @author capveg
 *
 */
public class OFSwitch extends FVMod
{
	String name;
	SocketChannel sock;
	public OFSwitch(String name, FVPollLoop pollLoop, SocketChannel sock)
	{
		super(name,pollLoop);
		this.sock = sock;
	}
}
