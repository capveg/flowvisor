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
public class FVHello extends org.openflow.protocol.OFHello 
		implements Classifiable, Slicable {

	/* (non-Javadoc)
	 * @see org.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.classifier.FVClassifier)
	 */
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// silently drop all Hello msgs
	}

	/* (non-Javadoc)
	 * @see org.flowvisor.message.Slicable#sliceFromController(org.flowvisor.classifier.FVClassifier, org.flowvisor.slicer.FVSlicer)
	 */
	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// silently drop all Hello msgs
	}

}
