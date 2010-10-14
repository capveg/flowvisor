package org.flowvisor.classifier;

import org.openflow.protocol.OFMessage;

public interface FVSendMsg {
	public void sendMsg(OFMessage msg);

	public String getConnectionName();
}
