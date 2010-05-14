package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;

public class FVFlowMod extends org.openflow.protocol.OFFlowMod 
		implements Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.translateXid(this, fvClassifier, fvSlicer);
		// FIXME: enforce flowspace isolation HERE
		fvClassifier.getMsgStream().write(this);
	}

}
