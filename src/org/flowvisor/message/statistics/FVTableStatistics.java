package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFTableStatistics;

public class FVTableStatistics extends OFTableStatistics implements
		SlicableStatistic, ClassifiableStatistic {

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		// TODO generate separate request/reply messages
		// TODO return the count of flows used by this slice
		FVMessageUtil.translateXid(msg, fvClassifier, fvSlicer);
		fvSlicer.sendMsg(msg);
	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		// TODO generate separate request/reply messages
		// TODO return the count of flows used by this slice
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer == null)
			FVLog.log(LogLevel.WARN, fvClassifier,
					"table stats: dropping unclassifiable msg: " + msg);
		else
			fvSlicer.sendMsg(msg);
	}

}
