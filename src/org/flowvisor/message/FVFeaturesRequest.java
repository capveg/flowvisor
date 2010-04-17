package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFeaturesRequest;

public class FVFeaturesRequest extends OFFeaturesRequest implements
		Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// should never get features requests from the switch
		// Log and drop
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg from switch: " + this);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// just rewrite XID before sending
		FVMessageUtil.translateXid(this, fvClassifier, fvSlicer);
		FVLog.log(LogLevel.DEBUG, fvClassifier, "sending to switch: " + this);
		fvClassifier.getMsgStream().write(this);
	}
}