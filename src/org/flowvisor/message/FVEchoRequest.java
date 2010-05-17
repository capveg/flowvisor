/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;

/**
 * Given an echo request, just send an immediate response from the fv
 *
 * FIXME consider sending these all the way through instead of faking
 * 	NEED to update regression tests
 *
 * @author capveg
 *
 */
public class FVEchoRequest extends org.openflow.protocol.OFEchoRequest
		implements Classifiable, Slicable {

	/* (non-Javadoc)
	 * @see org.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.classifier.FVClassifier)
	 */
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVEchoReply reply = new FVEchoReply();
		reply.setXid(this.getXid());
		fvClassifier.getMsgStream().write(reply);
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.message.Slicable#sliceFromController(org.flowvisor.classifier.FVClassifier, org.flowvisor.slicer.FVSlicer)
	 */
	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVEchoReply reply = new FVEchoReply();
		reply.setXid(this.getXid());
		fvSlicer.sendMsg(reply);
	}

}
