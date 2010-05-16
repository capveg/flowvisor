package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.statistics.ClassifiableStatistic;
import org.flowvisor.message.statistics.SlicableStatistic;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFQueueStatisticsReply;

public class FVQueueStatisticsReply extends OFQueueStatisticsReply implements
		ClassifiableStatistic, SlicableStatistic {


	@Override
	public void sliceFromController(OFMessage msg,FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + msg);
	}

	/**
	 * No need to rewrite response
	 */

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer == null )
			FVLog.log(LogLevel.WARN, fvClassifier, "dropping unclassifiable msg: " + msg);
		else {
			FVLog.log(LogLevel.DEBUG, fvSlicer, "sending to controller: " + msg);
			fvSlicer.getMsgStream().write(msg);
		}
	}


}
