package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFGetConfigRequest;

public class FVGetConfigRequest extends OFGetConfigRequest implements
		Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO Auto-generated method stub

	}

}
