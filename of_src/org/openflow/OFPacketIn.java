package org.openflow;

class OFPacketIn extends OFMessage
{
	public static int DEFAULT_CAPACITY = 4096;
    public OFPacketIn()
    {
        super(DEFAULT_CAPACITY);
        setType(OFPT_PACKET_IN);
    }
    public static void main(String args[])
    {
        OFPacketIn h = new OFPacketIn();
        System.out.println( "Test header: " + h);
    }
}
