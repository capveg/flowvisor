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
public class FVEchoRequest extends org.openflow.protocol.OFEchoRequest 
		implements Classifiable, Slicable {

	/* (non-Javadoc)
	 * @see org.flowvisor.message.Classifiable#classifyFromController(org.flowvisor.classifier.FVClassifier)
	 */
	@Override
	public void classifyFromController(FVClassifier fvClassifier) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.classifier.FVClassifier)
	 */
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.flowvisor.message.Slicable#sliceFromController(org.flowvisor.classifier.FVClassifier, org.flowvisor.slicer.FVSlicer)
	 */
	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.flowvisor.message.Slicable#sliceFromSwitch(org.flowvisor.classifier.FVClassifier, org.flowvisor.slicer.FVSlicer)
	 */
	@Override
	public void sliceFromSwitch(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO Auto-generated method stub

	}

}
