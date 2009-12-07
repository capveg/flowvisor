package org.openflow;

class OFSetConfig extends OFMessage
{
	final public static int OF_SETCONFIG_LENGTH = 12;
    public OFSetConfig()
    {
        super(OF_SETCONFIG_LENGTH);
        setType(OFPT_SET_CONFIG);
    }
    public static void main(String args[])
    {
        OFSetConfig h = new OFSetConfig();
        System.out.println( "Test header: " + h);
    }
}
