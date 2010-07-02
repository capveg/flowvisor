package org.flowvisor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.flowvisor.api.APIServer;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.ofswitch.OFSwitchAcceptor;
import org.flowvisor.ofswitch.TopologyController;

public class FlowVisor {
	// VENDOR EXTENSION ID
	public final static int FLOWVISOR_VENDOR_EXTENSION = 0x80000001;

	// VERSION
	public final static String FLOVISOR_VERSION = "flowvisor-0.6-alpha5";

	// Max slicename len ; used in LLDP for now; needs to be 1 byte
	public final static int MAX_SLICENAME_LEN = 255;

	/********/
	String configFile;
	ArrayList<FVEventHandler> handlers;

	private int port;
	static FlowVisor instance;

	public FlowVisor(String config[]) {
		this.configFile = config[0];
		this.port = 0;
		if (config.length > 1)
			this.port = Integer.valueOf(config[1]);
		this.handlers = new ArrayList<FVEventHandler>();
	}

	/*
	 * Unregister this event handler from the system
	 */

	public boolean unregisterHandler(FVEventHandler handler) {
		if (handlers.contains(handler)) {
			handlers.remove(handler);
			return true;
		}
		return false;
	}

	public void run() throws IOException, ConfigError, UnhandledEvent {
		// register this flowvisor instance as THE flowvisor instance
		FlowVisor.setInstance(this);

		// load config from file
		FVConfig.readFromFile(this.configFile);

		// init polling loop
		FVLog.log(LogLevel.INFO, null, "initializing poll loop");
		FVEventLoop pollLoop = new FVEventLoop();

		if (port == 0)
			port = FVConfig.getInt(FVConfig.LISTEN_PORT);

		// init topology discovery, if configured for it
		if (TopologyController.isConfigured())
			handlers.add(TopologyController.spawn(pollLoop));

		// init switchAcceptor
		OFSwitchAcceptor acceptor = new OFSwitchAcceptor(pollLoop, port, 16);
		handlers.add(acceptor);
		// start XMLRPC UserAPI server; FIXME not async!
		try {
			APIServer.spawn();
		} catch (Exception e) {
			FVLog.log(LogLevel.FATAL, null, "failed to spawn APIServer");
			e.printStackTrace();
			System.exit(-1);
		}
		// start event processing
		pollLoop.doEventLoop();

		/**
		 * FIXME add a cleanup call to event handlers // now shut everything
		 * down for (FVEventHandler fvh : handlers) fvh.cleanup();
		 */

	}

	/**
	 * FlowVisor Daemon Executable Main
	 * 
	 * Takes a config file as only parameter
	 * 
	 * @param args
	 *            config file
	 * @throws IOException
	 * @throws UnhandledEvent
	 * @throws ConfigError
	 */

	public static void main(String args[]) throws IOException, UnhandledEvent,
			ConfigError {

		// FIXME :: do real arg parsing
		if (args.length == 0)
			usage("need to specify config");

		FlowVisor fv = new FlowVisor(args);
		fv.run();
	}

	/**
	 * Print usage message and warning string then exit
	 * 
	 * @param string
	 *            warning
	 */

	private static void usage(String string) {
		System.err.println("err: " + string);
		System.err.println("Usage: FlowVisor configfile.xml [port]");
		System.exit(-1);
	}

	/**
	 * Get the running fv instance
	 * 
	 * @return
	 */
	public static FlowVisor getInstance() {
		return instance;
	}

	/**
	 * Set the running fv instance
	 * 
	 * @param instance
	 */
	public static void setInstance(FlowVisor instance) {
		FlowVisor.instance = instance;
	}

	public ArrayList<FVEventHandler> getHandlers() {
		return handlers;
	}

	public void addHandler(FVEventHandler handler) {
		this.handlers.add(handler);
	}

	public void removeHandler(FVEventHandler handler) {
		this.handlers.remove(handler);
	}

	public void setHandlers(ArrayList<FVEventHandler> handlers) {
		this.handlers = handlers;
	}

	/**
	 * Save the running config back to disk
	 * 
	 * Write to a temp file and only if it succeeds, move it into place
	 * 
	 * FIXME: add versioning
	 */
	public void checkPointConfig() {
		String tmpFile = this.configFile + ".tmp"; // assumes no one else can
		// write to same dir
		// else security problem

		// do we want checkpointing?
		try {
			if (!FVConfig.getBoolean(FVConfig.CHECKPOINTING))
				return;
		} catch (ConfigError e1) {
			FVLog
					.log(LogLevel.WARN, null,
							"Checkpointing config not set: assuming you want checkpointing");
		}

		try {
			FVConfig.writeToFile(tmpFile);
		} catch (FileNotFoundException e) {
			FVLog.log(LogLevel.CRIT, null,
					"failed to save config: tried to write to '" + tmpFile
							+ "' but got FileNotFoundException");
			return;
		}
		// sometimes, Java has the stoopidest ways of doing things :-(
		File tmp = new File(tmpFile);
		if (tmp.length() == 0) {
			FVLog.log(LogLevel.CRIT, null,
					"failed to save config: tried to write to '" + tmpFile
							+ "' but wrote empty file");
			return;
		}

		tmp.renameTo(new File(this.configFile));
		FVLog.log(LogLevel.INFO, null, "Saved config to disk at "
				+ this.configFile);
	}
}
