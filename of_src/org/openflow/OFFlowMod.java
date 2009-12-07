package org.openflow;

import org.openflow.util.LameUnsigned;

public class OFFlowMod extends OFMessage
{
	// might consider changing these to enums
	final public static int OFPFC_ADD			= 0;
	final public static int OFPFC_MODIFY		= 1;
	final public static int OFPFC_MODIFY_STRICT	= 2;
	final public static int OFPFC_DELETE		= 3;
	final public static int OFPFC_DELETE_STRICT = 4;
	
	static final int OFFSET_MATCH 			= OF_HEADER_SIZE;
	static final int OFFSET_COOKIE			= OFFSET_MATCH + OFMatch.OF_MATCH_SIZE;
	static final int OFFSET_COMMAND			= OFFSET_COOKIE + 8;
	static final int OF_FLOWMOD_SIZE		= 72;
	public static int DEFAULT_CAPACITY = 4096;
	
	OFMatch match;
	
    public OFFlowMod()
    {
        super(DEFAULT_CAPACITY);
        setType(OFPT_FLOW_MOD);
        setLength(OF_FLOWMOD_SIZE);
        match = new OFMatch(this.data, OFFSET_MATCH);
    }
    public static void main(String args[])
    {
        OFFlowMod h = new OFFlowMod();
        System.out.println( "Test header: " + h);
    }
    
    public OFMatch getMatch() { return match; }
    public void setMatch(OFMatch neomatch) 
    {
    	match.set(neomatch);
    }
    
    public int getCommand() { return LameUnsigned.getUnsignedShort(data, OFFSET_COMMAND);}
    public OFFlowMod setCommand(int cmd)
    {
    	LameUnsigned.putUnsignedShort(data, OFFSET_COMMAND, cmd);
    	return this;
    }
}
