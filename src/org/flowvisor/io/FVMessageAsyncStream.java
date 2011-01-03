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
		// OF messages are small, so this is
		// a big performance boost
		sock.socket().setTcpNoDelay(true);
		sock.socket().setSendBufferSize(1024 * 1024);
	}

	public void testAndWrite(OFMessage m) throws BufferFull,
			MalformedOFMessage, IOException {
		int len = m.getLengthU();
		if (this.outBuf.remaining() < len) {
			this.flush(); // try a quick write to flush buffer
			if (this.outBuf.remaining() < len)
				// buffer still full; probably a bug
				throw new BufferFull("wanted to write " + m.getLengthU()
						+ " bytes to " + outBuf.capacity()
						+ " byte buffer, but only have space for "
						+ outBuf.remaining() + " :: failed writing " + m);
		}
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
