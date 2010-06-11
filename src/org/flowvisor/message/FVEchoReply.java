package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;

public class FVEchoReply extends org.openflow.protocol.OFEchoReply implements
		Slicable, Classifiable {

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// Sliently ignore/drop echo replies
	}

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// Sliently ignore/drop echo replies
	}
}
