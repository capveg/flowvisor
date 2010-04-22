package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.message.Classifiable;
import org.flowvisor.message.Slicable;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFQueueStatisticsRequest;

public class FVQueueStatisticsRequest extends OFQueueStatisticsRequest
		implements Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO Auto-generated method stub

	}

}
