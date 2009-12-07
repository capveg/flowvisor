package org.openflow;

class OFStatsReply extends OFMessage
{
	public static int DEFAULT_CAPACITY = 4096;
    public OFStatsReply()
    {
        super(DEFAULT_CAPACITY);
        setType(OFPT_STATS_REPLY);
    }
    public static void main(String args[])
    {
        OFStatsReply h = new OFStatsReply();
        System.out.println( "Test header: " + h);
    }
}
