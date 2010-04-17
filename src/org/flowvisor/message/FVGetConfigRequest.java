package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFGetConfigRequest;

public class FVGetConfigRequest extends OFGetConfigRequest implements
		Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: " + this);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.translateXid(this, fvClassifier, fvSlicer);
		FVLog.log(LogLevel.DEBUG, fvSlicer, "sending to switch: " + this);
		fvClassifier.getMsgStream().write(this);
	}

}
