package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.message.Classifiable;
import org.flowvisor.message.Slicable;
import org.flowvisor.slicer.FVSlicer;

public class FVAggregateStatisticsRequest extends
		org.openflow.protocol.statistics.OFAggregateStatisticsRequest implements
		Slicable, Classifiable {

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// TODO Auto-generated method stub

	}

}
