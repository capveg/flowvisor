package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.message.statistics.ClassifiableStatistic;
import org.flowvisor.message.statistics.SlicableStatistic;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.*;

public class FVVendorStatistics extends OFVendorStatistics implements
		SlicableStatistic, ClassifiableStatistic {

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getLength() {
		// TODO Auto-generated method stub :: ????
		return 0;
	}
}
