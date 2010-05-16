/**
 *
 */
package org.flowvisor.message.lldp;

import org.flowvisor.message.FVPacketIn;
import org.flowvisor.message.FVPacketOut;
import org.openflow.util.StringByteSerializer;
import java.nio.ByteBuffer;

/**
 * @author capveg
 *
 */
public class LLDPTrailer {
	public final static int MAGIC = 0xdeadcafe;
	public final static byte LLDP_CHASSIS_ID_LOCAL = 7;
	public final static int MIN_LENGTH = 10;
	public final static int MAGIC_LEN = 4;
	public final static int SLICENAMELEN_LEN = 1;
	public final static int FLOWNAMELEN_LEN = 1;
	public final static int TLV_LEN = 2;
	public final static int CHASSIS_ID_LEN = 1;
	public final static int TRAILER_HEADER_LEN = MAGIC_LEN +
								SLICENAMELEN_LEN +
								FLOWNAMELEN_LEN +
								TLV_LEN +
								CHASSIS_ID_LEN;
	String sliceName;
	String flowVisorName;		// for cross-aggregate federated GENI identification

	public LLDPTrailer(String sliceName) {
		this.sliceName = sliceName;
		this.flowVisorName = "";
	}
	public LLDPTrailer(String sliceName, String flowVisorName){
		this.sliceName = sliceName;
		this.flowVisorName = flowVisorName;
	}



	public String getSliceName() {
		return sliceName;
	}
	public void setSliceName(String sliceName) {
		this.sliceName = sliceName;
	}
	public String getFlowVisorName() {
		return flowVisorName;
	}
	public void setFlowVisorName(String flowVisorName) {
		this.flowVisorName = flowVisorName;
	}
	/**
	 * Append this trailer to the packet out; update the length and everything
	 *
	 * @param po
	 */
	public void appendTo(FVPacketOut po) {

		int len = this.length();
		byte[] embedded = po.getPacketData();

		ByteBuffer newPacket = ByteBuffer.allocate(embedded.length + len);
		newPacket.put(embedded);

		short tlv = (short) (1 + (len << 7));
		newPacket.putShort(tlv);

		newPacket.put(LLDP_CHASSIS_ID_LOCAL);

		StringByteSerializer.writeTo(newPacket, sliceName.length()+1, sliceName);
		StringByteSerializer.writeTo(newPacket, flowVisorName.length()+1, flowVisorName);

		newPacket.put((byte) (sliceName.length()+1));
		newPacket.put((byte) (flowVisorName.length()+1));
		newPacket.putInt(MAGIC);

		po.setPacketData(newPacket.array());
	}

	/**
	 * Checks if the LLDP trailer exists
	 *        and if so, parses it and removes it from the packet
	 * @param po
	 * @return
	 */

	public static LLDPTrailer getTrailer(FVPacketIn pi) {
		ByteBuffer packet = ByteBuffer.wrap(pi.getPacketData());
		if (packet.capacity() < MIN_LENGTH)
			return null;
		// work backwards through the trailer
		int offset = packet.capacity() - MAGIC_LEN;
		if (packet.getInt(offset) != MAGIC)
			return null;	// didn't find MAGIC trailer
		offset -= FLOWNAMELEN_LEN;
		byte flowLen = packet.get(offset);
		offset -= SLICENAMELEN_LEN;
		byte sliceLen = packet.get(offset);
		offset -= flowLen + sliceLen;
		packet.position(offset);
		LLDPTrailer trailer =  new LLDPTrailer(
				StringByteSerializer.readFrom(packet, sliceLen),
				StringByteSerializer.readFrom(packet, flowLen)
				);
		byte[] newPacket = new byte[packet.capacity() - trailer.length()];
		packet.position(0);
		packet.get(newPacket);
		pi.setPacketData(newPacket);
		return trailer;
	}

	public int length() {
		// TRAILER_HEADER_LEN== 9 == 2 for TLV header + 1 for chassis id subtype
		// 	    + 4 for magic + 1 for sliceName len + 1 for flowVisor name len
		//      + 2 for each null to term the string
		return Math.min(512,TRAILER_HEADER_LEN + this.sliceName.length() + this.flowVisorName.length() + 2);
	}
}
