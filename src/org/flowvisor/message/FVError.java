/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;

/**
 * @author capveg
 *
 */
public class FVError extends org.openflow.protocol.OFError implements Classifiable, Slicable {

	/* (non-Javadoc)
	 * @see org.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.classifier.FVClassifier)
	 */
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.untranslateXidAndSend(this, fvClassifier);
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.message.Slicable#sliceFromController(org.flowvisor.classifier.FVClassifier, org.flowvisor.slicer.FVSlicer)
	 */
	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString()+";msg=" + offendingMsg.toString();
	}

}
