/**
 * 
 */
package org.flowvisor.message;

import org.openflow.protocol.*;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.factory.BasicFactory;
import org.flowvisor.message.actions.*;

/**
 * @author capveg
 *
 */
public class FVMessageFactory extends BasicFactory {
	@SuppressWarnings("unchecked") // not sure how to deal with this...
	// HACK to convert OFMessage* to FVMessage*
	static final Class convertMap[] = {
	    FVHello.class,
	    FVError.class,
	    FVEchoRequest.class,
	    FVEchoReply.class,
	    FVVendor.class,
	    FVFeaturesRequest.class,
	    FVFeaturesReply.class,
	    FVGetConfigRequest.class,
	    FVGetConfigReply.class,
	    FVSetConfig.class,
	    FVPacketIn.class,
	    FVFlowRemoved.class,
	    FVPortStatus.class,
	    FVPacketOut.class,
	    FVFlowMod.class,
	    FVPortMod.class,
	    FVStatisticsRequest.class,
	    FVStatisticsReply.class,
	    FVBarrierRequest.class,
	    FVBarrierReply.class		
	};

	
	@SuppressWarnings("unchecked")
	static final Class convertActionsMap[] = {
	    FVActionOutput.class,
	    FVActionVirtualLanIdentifier.class,
	    FVActionVirtualLanPriorityCodePoint.class,
	    FVActionStripVirtualLan.class,
	    FVActionDataLayerSource.class,
	    FVActionDataLayerDestination.class,
	    FVActionNetworkLayerSource.class,
	    FVActionNetworkLayerDestination.class,
	    FVActionNetworkTypeOfService.class,
	    FVActionTransportLayerSource.class,
	    FVActionTransportLayerDestination.class,
	    FVActionEnqueue.class,
	    FVActionVendor.class
	};
  
	
    @SuppressWarnings("unchecked")
	@Override
    public OFMessage getMessage(OFType t) {
      Class<? extends OFMessage> c = (Class<? extends OFMessage>) convertMap[t.getTypeValue()];
      try {
          return c.getConstructor(new Class[]{}).newInstance();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    }

    @SuppressWarnings("unchecked")
	@Override
    public OFAction getAction(OFActionType t) {
        Class<? extends OFAction> c = (Class<? extends OFAction>) convertActionsMap[t.getTypeValue()];
        try {
            return c.getConstructor(new Class[]{}).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    
}
