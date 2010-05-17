package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.statistics.ClassifiableStatistic;
import org.flowvisor.message.statistics.SlicableStatistic;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFDescriptionStatistics;

public class FVDescriptionStatistics extends OFDescriptionStatistics implements
		SlicableStatistic, ClassifiableStatistic {

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.translateXid(msg, fvClassifier, fvSlicer);
		FVLog.log(LogLevel.DEBUG, fvSlicer, "sending to switch: " + msg);
		fvClassifier.getMsgStream().write(msg);
	}


	/**
	 * NOTE: we no long do any DescriptionStatistics rewriting, now that 1.0 support
	 * dp_desc field.
	 */
	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		// TODO change this if FVDescription Request gets its own type
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer == null )
			FVLog.log(LogLevel.WARN, fvClassifier, "dropping unclassifiable msg: " + msg);
		else {
			fvSlicer.sendMsg(msg);
		}
	}

}
