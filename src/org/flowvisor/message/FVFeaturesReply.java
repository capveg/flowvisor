package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;

public class FVFeaturesReply extends org.openflow.protocol.OFFeaturesReply implements Classifiable,
		Slicable {

	@Override
	public void classifyFromController(FVClassifier fvClassifier) {
		// TODO Auto-generated method stub

	}

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sliceFromSwitch(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO Auto-generated method stub

	}

}
