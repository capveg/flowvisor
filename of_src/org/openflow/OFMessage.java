package org.openflow;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.io.*;
import org.openflow.util.LameUnsigned;


public class OFMessage
{
    ByteBuffer data;
    static boolean sanityCheck              = true;
    static long XID                         = 1;

    final public int  OFP_VERSION                   = 0x98;
    final public int  OFP_MAX_TABLE_NAME_LEN        = 32;
    final public int  OFP_MAX_PORT_NAME_LEN         = 16;
    final public int  OFP_TCP_PORT                  = 6633;
    final public int  OFP_SSL_PORT                  = 6633;
    final public int  OFP_ETH_ALEN                  = 6;          /* Bytes in an Ethernet address. */
    final public int  OFP_DEFAULT_MISS_SEND_LEN     = 128;
    final public int  OFP_DL_TYPE_ETH2_CUTOFF       = 0x0600;
    final public int  OFP_DL_TYPE_NOT_ETH_TYPE      = 0x05ff;
    final public int  OFP_VLAN_NONE                 = 0xffff;
    final public int  OFP_FLOW_PERMANENT            = 0;
    final public int  OFP_DEFAULT_PRIORITY          = 0x8000;
    final public int  DESC_STR_LEN                  = 256;
    final public int  SERIAL_NUM_LEN                = 32;

/* OpenFlow message types */
	final public int    OFPT_HELLO          = 0;               /* Symmetric message */
	final public int    OFPT_ERROR          = 1;               /* Symmetric message */
	final public int    OFPT_ECHO_REQUEST   = 2;        /* Symmetric message */
	final public int    OFPT_ECHO_REPLY     = 3;          /* Symmetric message */
	final public int    OFPT_VENDOR			= 4;              /* Symmetric message */
	/* Switch configuration messages. */
	final public int    OFPT_FEATURES_REQUEST   = 5 ;   /* Controller/switch message */
	final public int    OFPT_FEATURES_REPLY     = 6 ;     /* Controller/switch message */
	final public int    OFPT_GET_CONFIG_REQUEST = 7 ; /* Controller/switch message */
	final public int    OFPT_GET_CONFIG_REPLY   = 8 ;   /* Controller/switch message */
	final public int    OFPT_SET_CONFIG			= 9 ;         /* Controller/switch message */
	/* Asynchronous messages. */
	final public int    OFPT_PACKET_IN			= 10;           /* Async message */
	final public int    OFPT_FLOW_REMOVED	    = 11;        /* Async message */
	final public int    OFPT_PORT_STATUS	    = 12;         /* Async message */
	/* Controller command messages. */
	final public int    OFPT_PACKET_OUT			= 13;          /* Controller/switch message */
	final public int    OFPT_FLOW_MOD			= 14;            /* Controller/switch message */
	final public int    OFPT_PORT_MOD			= 15;            /* Controller/switch message */
	/* Statistics messages. */
	final public int    OFPT_STATS_REQUEST	    = 16;       /* Controller/switch message */
	final public int    OFPT_STATS_REPLY	   	= 17;         /* Controller/switch message */
	/* Barrier messages. */
	final public int    OFPT_BARRIER_REQUEST    = 18;     /* Controller/switch message */
	final public int    OFPT_BARRIER_REPLY      = 19;  /* Controller/switch message */


    final public static String typeToString[] = 
        {
            "hello",
            "error",               /* Symmetric message */
            "echo_request",        /* Symmetric message */
            "echo_reply",          /* Symmetric message */
            "vendor",              /* Symmetric message */
            /* Switch configuration messages. */
            "features_request",    /* Controller/switch message */
            "features_reply",      /* Controller/switch message */
            "config_request",  /* Controller/switch message */
            "config_reply",    /* Controller/switch message */
            "config_set",          /* Controller/switch message */
            /* Asynchronous messages. */
            "packet_in",           /* Async message */
            "flow_removed",        /* Async message */
            "port_status",         /* Async message */
            /* Controller command messages. */
            "packet_out",          /* Controller/switch message */
            "flow_mod",            /* Controller/switch message */
            "port_mod",            /* Controller/switch message */
            /* Statistics messages. */
            "stats_request",       /* Controller/switch message */
            "stats_reply",         /* Controller/switch message */
            /* Barrier messages. */
            "barrier_request",     /* Controller/switch message */
            "barrier_reply"        /* Controller/switch message */
        };


    final static public int OF_HEADER_SIZE         = 8;

    final int OFFSET_VERSION                = 0;
    final int OFFSET_TYPE                   = OFFSET_VERSION    + 1;
    final int OFFSET_LENGTH                 = OFFSET_TYPE       + 1;
    final int OFFSET_XID                    = OFFSET_LENGTH     + 2;

    public OFMessage(int len)
    {
        this.data = ByteBuffer.allocate(len);
        this.setVersion(OFP_VERSION);
        // type defaults to 0 which is Hello
        this.setLength(len);
        this.setXID(createXID());
    }

    public OFMessage(ByteBuffer msg)
    {
        data = msg;
        if(sanityCheck)
        {
            if(data.limit() < OF_HEADER_SIZE)
                throw new IllegalArgumentException("malformed openflow message: too short");
        }
    }

    private static synchronized long createXID() {
        return XID++;
    }

    public OFMessage setVersion(int version)
    {
        LameUnsigned.putUnsignedByte(data,OFFSET_VERSION,version);
        return this;
    }
    public int getVersion()
    {
        return LameUnsigned.getUnsignedByte(data,OFFSET_VERSION);
    }

    public OFMessage setType(int type)
    {
        LameUnsigned.putUnsignedByte(data,OFFSET_TYPE, type);
        return this;
    }

    public int getType()
    {
        return LameUnsigned.getUnsignedByte(data,OFFSET_TYPE);
    }

    public int getLength()
    {
    	return LameUnsigned.getUnsignedShort(this.data, OFFSET_LENGTH);
    }
    public OFMessage setLength(int length)
    {
        LameUnsigned.putUnsignedShort(data,OFFSET_TYPE, length);
        data.position(length);
        return this;
    }

    public long getXID()
    {
        return LameUnsigned.getUnsignedInt(data,OFFSET_XID);
    }

    public OFMessage setXID(long XID)
    {
        
        LameUnsigned.putUnsignedInt(data,OFFSET_XID, XID);
        return this;
    }

    public String toString()
    {
        return "OF:v=" + getVersion() + ":t=" + typeToString[getType()] + ":l=" + getLength() + ":x=" + getXID();
    }

    public static void main(String args[])
    {
        OFMessage h = new OFMessage(OFMessage.OF_HEADER_SIZE);
        System.out.println( "Test header: " + h);
    }

	public int writeToSocketChannel(SocketChannel sock) throws IOException
	{
		// save the buffer's state variables
		int limit = this.data.limit();
		int position = this.data.position();
		this.data.flip(); // mess the state variables up for the write

        // FIXME this does not necessarily write all the data in the buffer - won't work when the socket blocks
		int count = sock.write(this.data);

		// and put them back like we weren't here -- horrible
		this.data.limit(limit);
		this.data.position(position);
		return count;
	}
}
