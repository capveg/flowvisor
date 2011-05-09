package org.flowvisor.message.statistics;

import java.nio.ByteBuffer;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFVendorStatistics;

public class FVVendorStatistics extends OFVendorStatistics implements
		SlicableStatistic, ClassifiableStatistic {

	OFMessage vendor; // weird protocol thing; a vendor_stats message has an

	// openflow message header followed by a body

	@Override
	public void readFrom(ByteBuffer data) {
		vendor = new OFMessage();
		// this should read the header AND body of the stat
		vendor.readFrom(data);
	}

	@Override
	public void writeTo(ByteBuffer data) {
		if (vendor != null)
			vendor.writeTo(data);
	}

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		FVMessageUtil.untranslateXidAndSend(msg, fvClassifier);
	}

	@Override
	public int getLength() {
		return vendor.getLengthU();
	}
}
