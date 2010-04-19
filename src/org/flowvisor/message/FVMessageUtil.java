/**
 * 
 */
package org.flowvisor.message;

import java.util.ArrayList;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.XidPair;
import org.flowvisor.classifier.XidTranslator;
import org.flowvisor.slicer.*;
import org.flowvisor.message.actions.*;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.action.OFAction;


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

	
	/**
	 * Is this slice allowed to use this list of actions with this ofmatch structure?
	 * 
	 * Return a (potentially edited) list of actions or throw an exception if not allowed
	 * 
	 * @param actionList 
	 * @param match inPort is encapsulated in the match
	 * @param fvClassifier
	 * @param fvSlicer
	 * @return A list of actions the slice is actually allowed to send
	 * @throws ActionDisallowedException
	 */
	static public List<OFAction> approveActions(List<OFAction> actionList, OFMatch match, 
				FVClassifier fvClassifier, FVSlicer fvSlicer) throws ActionDisallowedException {
		List<OFAction> approvedList = new ArrayList<OFAction>();
		
		if (actionList == null)
			return null;
		for(OFAction action : actionList ) 
			((SlicableAction)action).slice(approvedList, match, fvClassifier, fvSlicer);
		return approvedList;	
	}
}
