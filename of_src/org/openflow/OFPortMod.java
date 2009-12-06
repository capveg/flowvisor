package org.openflow;

class OFPortMod extends OFMessage
{
    public OFPortMod()
    {
        super();
        setType(OFPT_PORT_MOD);
    }
    public static void main(String args[])
    {
        OFPortMod h = new OFPortMod();
        System.out.println( "Test header: " + h);
    }
}
