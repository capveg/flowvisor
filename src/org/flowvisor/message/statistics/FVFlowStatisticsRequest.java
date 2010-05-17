package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.statistics.ClassifiableStatistic;
import org.flowvisor.message.statistics.SlicableStatistic;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;

public final class FVFlowStatisticsRequest extends OFFlowStatisticsReply
		implements SlicableStatistic, ClassifiableStatistic {


	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO: rewrite/sanity check this request against the flowspace
		FVLog.log(LogLevel.WARN, fvSlicer, "need to implement flowstats request slicing");
		FVMessageUtil.translateXid(msg, fvClassifier, fvSlicer);
		FVLog.log(LogLevel.DEBUG, fvSlicer, "sending to switch: " + msg);
		fvSlicer.sendMsg(msg);
	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: " + msg);
	}

}
