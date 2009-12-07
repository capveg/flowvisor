package org.openflow;

class OFPortMod extends OFMessage
{
	final public static int OF_PORTMOD_LENGTH = 32;
    public OFPortMod()
    {
        super(OF_PORTMOD_LENGTH);
        setType(OFPT_PORT_MOD);
    }
    public static void main(String args[])
    {
        OFPortMod h = new OFPortMod();
        System.out.println( "Test header: " + h);
    }
}
