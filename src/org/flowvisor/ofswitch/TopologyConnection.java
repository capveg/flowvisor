/**
 * 
 */
package org.flowvisor.ofswitch;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.api.LinkAdvertisement;
import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageFactory;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
 * @author capveg
 *
 */
public class TopologyConnection implements FVEventHandler {
	
	TopologyController topologyController;
	FVEventLoop pollLoop;
	SocketChannel sock;
	boolean isConnected;
	String name;
	OFMessageAsyncStream msgStream;
	FVMessageFactory fvMessageFactory;
	
	public TopologyConnection(TopologyController topologyController, FVEventLoop pollLoop, SocketChannel sock) {
		this.topologyController = topologyController;
		this.pollLoop = pollLoop;
		this.sock = sock;
		this.isConnected = false;	// not yet initialized
		this.name = "topo." + sock.toString();
		this.fvMessageFactory = new FVMessageFactory();
		try {
			this.msgStream = new OFMessageAsyncStream(sock, this.fvMessageFactory);
		} catch (IOException e) {
			FVLog.log(LogLevel.CRIT, this, "IOException in constructor!");
			e.printStackTrace();
		}
	}



	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#getThreadContext()
	 */
	@Override
	public long getThreadContext() {
		return Thread.currentThread().getId();
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#handleEvent(org.flowvisor.events.FVEvent)
	 */
	@Override
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#needsAccept()
	 */
	@Override
	public boolean needsAccept() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#needsConnect()
	 */
	@Override
	public boolean needsConnect() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#needsRead()
	 */
	@Override
	public boolean needsRead() {
		return true;		// always interested if there is something to read
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#needsWrite()
	 */
	@Override
	public boolean needsWrite() {
		if (this.msgStream == null)
			return false;
		else
			return msgStream.needsFlush();
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.events.FVEventHandler#tearDown()
	 */
	@Override
	public void tearDown() {
		try {
			sock.close();
		} catch (IOException e) {
			FVLog.log(LogLevel.ALERT, this, "ignoring error on shutdown: " + e);
		}
	}

	/**
	 * Setup the connection
	 * 
	 * Queue up OFHello(), OFFeatureRequest(), and Stats Desc request messages
	 * @throws IOException 
	 */
	public void init() throws IOException {
		msgStream.write(this.fvMessageFactory.getMessage(OFType.HELLO));
		msgStream.write(this.fvMessageFactory.getMessage(OFType.FEATURES_REQUEST));
		
		// build stats desc request : FIXME: make this cleaner
		OFStatisticsRequest request = (OFStatisticsRequest) this.fvMessageFactory.getMessage(OFType.STATS_REQUEST);
		List<OFStatistics> statistics = new LinkedList<OFStatistics>();
		statistics.add(this.fvMessageFactory.getStatistics(OFType.STATS_REQUEST, OFStatisticsType.DESC));
		request.setStatistics(statistics);
		msgStream.write(request);
		
		msgStream.flush();
		int ops = SelectionKey.OP_READ;
		if (msgStream.needsFlush())
			ops |= SelectionKey.OP_WRITE;
		this.pollLoop.register(sock, ops, this);	
	}


	/** 
	 * Return whether this topology instance is fully connected and initialized.
	 * 
	 * Specifically, do we have responses to the initial features request and stats.desc requests
	 * @return
	 */

	public boolean isConnected() {
		return this.isConnected;
	}



	public Long getDataPathID() {
		// TODO Auto-generated method stub
		return null;
	}



	public void getLinks(List<LinkAdvertisement> links) {
		// TODO Auto-generated method stub
		
	}

}
