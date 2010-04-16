package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFeaturesRequest;

public class FVFeaturesRequest extends OFFeaturesRequest implements
		Classifiable, Slicable {

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
