package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFPortStatus;

/**
 * Send the port status message to each slice that uses this port
 * 
 * @author capveg
 *
 */

public class FVPortStatus extends OFPortStatus implements Classifiable,
		Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		Short port = Short.valueOf(this.getDesc().getPortNumber());
		byte mac[] = this.getDesc().getHardwareAddress();
		FVLog.log(LogLevel.DEBUG, fvClassifier, "port status mac = " + 
				String.format("%1$x:%2$x:%3$x:%4$x:%5$x:%6$x", 
				mac[0], mac[1], mac[2], mac[3], mac[4],mac[5]		
				));
		for(FVSlicer fvSlicer: fvClassifier.getSlicerMap().values()) {
			if (fvSlicer.getPorts().contains(port)) {
				FVLog.log(LogLevel.DEBUG, fvSlicer, "sending msg to controller: " + this);
				fvSlicer.getMsgStream().write(this);
			}
		}
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + this);
	}

}
