package org.openflow;

import java.nio.*;

import org.openflow.util.LameUnsigned;
/******
 * An OpenFlow match structure
 * 
 * @author capveg
 *
 */

public class OFMatch {
	public final static int OF_MATCH_SIZE = 40;

	ByteBuffer matchBuf;// a pointer to the packet buffer this match lives in
	int matchOffset;	// the offset of where the match starts
	
	final public static int OFPFW_IN_PORT  = 1 << 0;  /* Switch input port. */
	final public static int OFPFW_DL_VLAN  = 1 << 1;  /* VLAN id. */
	final public static int OFPFW_DL_SRC   = 1 << 2;  /* Ethernet source address. */
	final public static int OFPFW_DL_DST   = 1 << 3;  /* Ethernet destination address. */
	final public static int OFPFW_DL_TYPE  = 1 << 4;  /* Ethernet frame type. */
	final public static int OFPFW_NW_PROTO = 1 << 5;  /* IP protocol. */
	final public static int OFPFW_TP_SRC   = 1 << 6;  /* TCP/UDP source port. */
	final public static int OFPFW_TP_DST   = 1 << 7;  /* TCP/UDP destination port. */

    /* IP source address wildcard bit count.  0 is exact match; 1 ignores the
     * LSB; 2 ignores the 2 least-significant bits; ...; 32 and higher wildcard
     * the entire field.  This is the *opposite* of the usual convention where
     * e.g. /24 indicates that 8 bits (not 24 bits) are wildcarded. */
	final public static int OFPFW_NW_SRC_SHIFT = 8;
	final public static int OFPFW_NW_SRC_BITS = 6;
	final public static int OFPFW_NW_SRC_MASK = ((1 << OFPFW_NW_SRC_BITS) - 1) << OFPFW_NW_SRC_SHIFT;
	final public static int OFPFW_NW_SRC_ALL = 32 << OFPFW_NW_SRC_SHIFT;

    /* IP destination address wildcard bit count.  Same format as source. */
	final public static int OFPFW_NW_DST_SHIFT = 14;
	final public static int OFPFW_NW_DST_BITS = 6;
	final public static int OFPFW_NW_DST_MASK = ((1 << OFPFW_NW_DST_BITS) - 1) << OFPFW_NW_DST_SHIFT;
	final public static int OFPFW_NW_DST_ALL = 32 << OFPFW_NW_DST_SHIFT;

	final public static int OFPFW_DL_VLAN_PCP = 1 << 20;  /* VLAN priority. */
	final public static int OFPFW_NW_TOS = 1 << 21;  /* IP ToS (DSCP field; 6 bits). */

    /* Wildcard all fields. */
	final public static int OFPFW_ALL = ((1 << 22) - 1);

	final static int OFFSET_WILDCARDS = 0;
	
	/***
	 * Store match information locally
	 */
	public OFMatch()
	{
		this.matchBuf = ByteBuffer.allocate(OF_MATCH_SIZE);
		this.matchBuf.position(OF_MATCH_SIZE);
		this.matchOffset=0;
	}
	/***
	 * Tie this match to an externally allocated ByteBuffer, i.e., as part of another 
	 * 	openflow message
	 * @param buf A byte buffer that is larger than OF_MATCH_SIZE + offset
	 * @param offset The offset into buf where the match begins, e.g., with flow_mod, offset=8
	 */
	public OFMatch(ByteBuffer buf, int offset)
	{
		this.matchBuf = buf;
		this.matchOffset = offset;
	}
	
	public void set(OFMatch match)
	{
		// forcibly and efficiently overwrite the current match with the passed one
		System.arraycopy(
				this.matchBuf.array(),
				this.matchOffset,
				match.matchBuf.array(),
				match.matchOffset,
				OF_MATCH_SIZE);
	}
	/***
	 * Generate an OFMatch that matches everything
	 * @return An OFMatch that matches everything
	 */
	public static OFMatch makeMatchAll()
	{
		OFMatch match = new OFMatch();
		match.setWildcards(OFPFW_ALL);
		// FIXME: understand more java and decide if the rest of the match needs to be zero'd
		return match;
	}
	
	public OFMatch setWildcards(long wildcards)
	{
		LameUnsigned.putUnsignedInt(matchBuf, matchOffset+ OFFSET_WILDCARDS, wildcards);
		return this;
	}

	
}
