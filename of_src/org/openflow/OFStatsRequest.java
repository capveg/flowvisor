package org.openflow;

class OFStatsRequest extends OFMessage
{
	public static int DEFAULT_CAPACITY=4096;
    public OFStatsRequest()
    {
        super(DEFAULT_CAPACITY);
        setType(OFPT_STATS_REQUEST);
    }
    public static void main(String args[])
    {
        OFStatsRequest h = new OFStatsRequest();
        System.out.println( "Test header: " + h);
    }
}
