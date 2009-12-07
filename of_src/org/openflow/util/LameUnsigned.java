package org.openflow.util;

import java.nio.ByteBuffer;
/*****
 * OMG is this Lame... no unsigned's in java and can't extend ByteBuffer
 * ... sigh
 * @author capveg
 *
 */


public class LameUnsigned {
	public static void putUnsignedByte (ByteBuffer bb, int v)
	{
		bb.put ((byte)(v & 0xff));
	}
		
	public static short getUnsignedByte (ByteBuffer bb, int offset)
	{
		return ((short)(bb.get(offset) & (short)0xff));
	}
		
	public static void putUnsignedByte (ByteBuffer bb, int offset, int v)
	{
		bb.put(offset, (byte)( v & 0xff));
	}
	
	public static int getUnsignedShort (ByteBuffer bb)
	{
		return (bb.getShort() & 0xffff);
	}
	
	public static void putUnsignedShort (ByteBuffer bb, int  v)
	{
		bb.putShort ((short)( v & 0xffff));
	}
	
	public static int getUnsignedShort (ByteBuffer bb, int position)
	{
		return (bb.getShort (position) & 0xffff);
	}
	
	public static void putUnsignedShort (ByteBuffer bb, int position, int  v)
	{
		bb.putShort (position, (short)( v & 0xffff));
	}
		
	public static long getUnsignedInt (ByteBuffer bb)
	{
		return ((long)bb.getInt() & 0xffffffffL);
	}
	
	public static void putUnsignedInt (ByteBuffer bb, long  v)
	{
		bb.putInt ((int)( v & 0xffffffffL));
	}
	
	public static long getUnsignedInt (ByteBuffer bb, int position)
	{
		return ((long)bb.getInt (position) & 0xffffffffL);
	}
	
	public static void putUnsignedInt (ByteBuffer bb, int position, long  v)
	{
		bb.putInt (position, (int)( v & 0xffffffffL));
	}	
}

