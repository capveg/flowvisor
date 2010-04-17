/**
 * 
 */
package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.XidPair;
import org.flowvisor.classifier.XidTranslator;
import org.openflow.protocol.OFMessage;
import org.flowvisor.slicer.*;

/**
 * @author capveg
 *
 */
public class FVMessageUtil {

	/**
	 * Translate the XID of a message from controller-unique to switch unique
	 * Also, record the <oldXid,FVSlicer> mapping so we can reverse this later
	 * 
	 * @param msg
	 * @param fvClassifier
	 * @param fvSlicer
	 */
	static public void translateXid(OFMessage msg, FVClassifier fvClassifier, 
					FVSlicer fvSlicer) {
		XidTranslator xidTranslator = fvClassifier.getXidTranslator();
		int newXid = xidTranslator.translate(msg.getXid(), fvSlicer);
		msg.setXid(newXid);
	}
	
	/**
	 * Undo the effect of translateXID, and return the FVSlicer this came from
	 * @param msg
	 * @param fvClassifier
	 * @return the fvSlicer that was input in the translate step
	 */
	static public FVSlicer untranslateXid(OFMessage msg, FVClassifier fvClassifier) {
		XidTranslator xidTranslator = fvClassifier.getXidTranslator();
		XidPair pair = xidTranslator.untranslate(msg.getXid());
		msg.setXid(pair.getXid());
		return pair.getFvSlicer();
	}
}
