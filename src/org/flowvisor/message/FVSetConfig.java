package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFSetConfig;

public class FVSetConfig extends OFSetConfig implements Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpect msg: " + this);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		int missSendLength = this.getMissSendLength();
		fvSlicer.setMissSendLength(missSendLength);
		if (fvClassifier.getMissSendLength() < missSendLength) {
			fvClassifier.setMissSendLength(missSendLength);
			FVLog.log(LogLevel.DEBUG, fvClassifier, "sending to switch: " + this);
		}
	}

}
