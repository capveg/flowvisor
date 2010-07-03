/**
 *
 */
package org.flowvisor.message.lldp;

import java.nio.ByteBuffer;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVPacketIn;
import org.flowvisor.message.FVPacketOut;
import org.flowvisor.slicer.FVSlicer;

/**
 * Set of utilities for handling our LLDP virtualization hacks
 * 
 * @author capveg
 * 
 */
public class LLDPUtil {
	final public static short ETHER_LLDP = (short) 0x88cc;
	final public static short ETHER_VLAN = (short) 0x8100;
	final public static byte[] LLDP_MULTICAST = { 0x01, 0x23, 0x20, 0x00, 0x00,
			0x01 };

	/**
	 * If this msg is lldp, then 1) add a slice identifying trailer 2) send to
	 * switch -- all slices can send lldp, no matter flowspace 3) return true,
	 * we've handled this packet else return false
	 * 
	 * @param po
	 *            message
	 * @param fvClassifier
	 *            switch classifier
	 * @param fvSlicer
	 *            slice polcies
	 * @return did we handle the message?
	 */
	static public boolean handleLLDPFromController(FVPacketOut po,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		if (!LLDPCheck(po.getPacketData()))
			return false;
		LLDPTrailer trailer = new LLDPTrailer(fvSlicer.getSliceName(), "fv1");
		trailer.appendTo(po);
		FVLog.log(LogLevel.DEBUG, fvSlicer, "applied lldp hack: " + po);
		fvClassifier.getMsgStream().write(po);
		return true;
	}

	/**
	 * Is this an lldp packet?
	 * 
	 * @param po
	 * @return
	 */

	static private boolean LLDPCheck(byte[] packetArray) {
		if ((packetArray == null) || (packetArray.length < 14))
			return false; // not lddp if no packet exists or too short
		ByteBuffer packet = ByteBuffer.wrap(packetArray);
		short ether_type = packet.getShort(12);
		if (ether_type == ETHER_VLAN)
			ether_type = packet.getShort(16);
		if (ether_type != ETHER_LLDP)
			return false;
		// TODO think about checking for NOX OID
		return true;
	}

	/**
	 * If this msg is lldp, then 1) remove the slice identifying trailer 2) send
	 * to controller -- all slices can send lldp, no matter flowspace 3) return
	 * true, we've handled this packet
	 * 
	 * @param po
	 * @param fvClassifier
	 * @return did we handle this message?
	 */
	static public boolean handleLLDPFromSwitch(FVPacketIn pi,
			FVClassifier fvClassifier) {
		if (!LLDPCheck(pi.getPacketData()))
			return false;
		LLDPTrailer trailer = LLDPTrailer.getTrailer(pi);
		if (trailer == null)
			return false;
		FVSlicer fvSlicer = fvClassifier
				.getSlicerByName(trailer.getSliceName());
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"failed to undo llpd hack for unknown slice '"
							+ trailer.getSliceName() + "': " + pi);
			return false;
		}
		FVLog.log(LogLevel.DEBUG, fvSlicer, "undoing lldp hack: " + pi);
		fvSlicer.sendMsg(pi);
		return true;
	}
}
