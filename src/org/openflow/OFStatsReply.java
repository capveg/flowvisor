package org.openflow;

class OFStatsReply extends OFMessage
{
    public OFStatsReply()
    {
        super();
        setType(OFPT_STATS_REPLY);
    }
    public static void main(String args[])
    {
        OFStatsReply h = new OFStatsReply();
        System.out.println( "Test header: " + h);
    }
}
