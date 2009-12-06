package org.openflow;

class OFStatsRequest extends OFMessage
{
    public OFStatsRequest()
    {
        super();
        setType(OFPT_STATS_REQUEST);
    }
    public static void main(String args[])
    {
        OFStatsRequest h = new OFStatsRequest();
        System.out.println( "Test header: " + h);
    }
}
