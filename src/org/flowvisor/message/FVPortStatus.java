package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFPortStatus;
import org.openflow.util.HexString;

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
		FVLog.log(LogLevel.DEBUG, fvClassifier, "port status mac = " +
				HexString.toHexString(this.getDesc().getHardwareAddress()));
		for(FVSlicer fvSlicer: fvClassifier.getSlicerMap().values()) {
			if (fvSlicer.portInSlice(port)) {
				fvSlicer.sendMsg(this);
			}
		}
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}
}
