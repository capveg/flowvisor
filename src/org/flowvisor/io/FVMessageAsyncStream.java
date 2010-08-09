package org.flowvisor.io;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.flowvisor.exceptions.BufferFull;
import org.flowvisor.exceptions.MalformedOFMessage;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.factory.OFMessageFactory;

public class FVMessageAsyncStream extends OFMessageAsyncStream {
	public FVMessageAsyncStream(SocketChannel sock,
			OFMessageFactory messageFactory) throws IOException {
		super(sock, messageFactory);
	}

	public void testAndWrite(OFMessage m) throws BufferFull, MalformedOFMessage {
		int len = m.getLengthU();
		if (this.outBuf.remaining() < len)
			throw new BufferFull("wanted to write " + m.getLengthU()
					+ " bytes but only have space for " + outBuf.remaining());
		int start = this.outBuf.position();
		super.write(m);
		int wrote = this.outBuf.position() - start;
		if (len != wrote) { // was the packet correctly written
			// no! back it out and throw an error
			this.outBuf.position(start);
			FVLog.log(LogLevel.CRIT, null, "dropping bad OF Message: " + m);
			throw new MalformedOFMessage("len=" + len + ",wrote=" + wrote
					+ " msg=" + m);
		}
	}

}
