package org.flowvisor.io;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.flowvisor.Exception.BufferFull;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.factory.OFMessageFactory;

public class FVMessageAsyncStream extends OFMessageAsyncStream {
	public FVMessageAsyncStream(SocketChannel sock,
			OFMessageFactory messageFactory) throws IOException {
		super(sock, messageFactory);
	}

	public void testAndWrite(OFMessage m) throws BufferFull {
		if (this.outBuf.remaining() < m.getLengthU())
			throw new BufferFull("wanted to write " + m.getLengthU()
					+ " bytes but only have space for " + outBuf.remaining());
		super.write(m);
	}

}
