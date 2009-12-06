package org.openflow;

class OFPacketIn extends OFMessage
{
    public OFPacketIn()
    {
        super();
        setType(OFPT_PACKET_IN);
    }
    public static void main(String args[])
    {
        OFPacketIn h = new OFPacketIn();
        System.out.println( "Test header: " + h);
    }
}
