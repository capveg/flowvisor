package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;

public class FVFeaturesReply extends org.openflow.protocol.OFFeaturesReply implements Classifiable,
		Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(this, fvClassifier);
		if(fvSlicer == null ) {
			FVLog.log(LogLevel.WARN, fvClassifier, 
					" dropping msg with un-untranslatable xid: " +
					this);
			return;
		}
		this.prunePorts(fvSlicer);		// remove ports that are not part of slice
		// TODO: rewrite DPID if this is a virtual switch
		FVLog.log(LogLevel.DEBUG, fvSlicer, "sending to controller: " + this);
		fvSlicer.getMsgStream().write(this);
	}

	private void prunePorts(FVSlicer fvSlicer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// Should never get Features Reply from controller
		// Log and drop
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg from controller: "
				+ this);
	}

}
