package org.openflow;

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
	
	public static int DEFAULT_CAPACITY = 4096;
	
	OFMatch match;
	
    public OFFlowMod()
    {
        super(DEFAULT_CAPACITY);
        setType(OFPT_FLOW_MOD);
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
}
