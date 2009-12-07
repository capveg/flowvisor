package org.openflow;

import java.nio.*;
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
	
}
