package org.flowvisor.message;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.statistics.SlicableStatistic;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;

public class FVStatisticsRequest extends OFStatisticsRequest implements
		Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: " + this);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO: come back and retool FV stats handling to make this less fugly
		List<OFStatistics> statsList = this.getStatistics();
		if(statsList.size() > 0) {	// if there is a body, do body specific parsing
			OFStatistics stat = statsList.get(0);
			assert(stat instanceof SlicableStatistic);
			((SlicableStatistic)stat).sliceFromController(this, fvClassifier, fvSlicer);
		} else {
			// else just slice by xid and hope for the best
			FVMessageUtil.translateXid(this, fvClassifier, fvSlicer);
			FVLog.log(LogLevel.DEBUG, fvClassifier, "sending to switch: " + this);
			fvClassifier.getMsgStream().write(this);
		}
	}

	@Override
	public String toString() {	
		return super.toString() + 
			";st=" + this.getStatisticType(); 
			// ";mfr=" + this.getManufacturerDescription() + 
	}
	
}
