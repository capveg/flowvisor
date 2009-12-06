package org.openflow;

class OFPacketOut extends OFMessage
{
    public OFPacketOut()
    {
        super();
        setType(OFPT_PACKET_OUT);
    }
    public static void main(String args[])
    {
        OFPacketOut h = new OFPacketOut();
        System.out.println( "Test header: " + h);
    }
}
