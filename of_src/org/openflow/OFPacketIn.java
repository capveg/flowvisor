package org.openflow;

class OFPacketIn extends OFMessage
{
	public static int DEFAULT_CAPACITY = 4096;
	final public static int OF_PACKETIN_SIZE = 20;
    public OFPacketIn()
    {
        super(DEFAULT_CAPACITY);
        setType(OFPT_PACKET_IN);
        setLength(OF_PACKETIN_SIZE);
    }
    public static void main(String args[])
    {
        OFPacketIn h = new OFPacketIn();
        System.out.println( "Test header: " + h);
    }
}
