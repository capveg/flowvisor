package org.openflow;

class OFPacketOut extends OFMessage
{
	public static int DEFAULT_CAPACITY=4096;
    public OFPacketOut()
    {
        super(DEFAULT_CAPACITY);
        setType(OFPT_PACKET_OUT);
    }
    public static void main(String args[])
    {
        OFPacketOut h = new OFPacketOut();
        System.out.println( "Test header: " + h);
    }
}
