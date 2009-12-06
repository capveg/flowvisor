package org.openflow;

class OFSetConfig extends OFMessage
{
    public OFSetConfig()
    {
        super();
        setType(OFPT_SET_CONFIG);
    }
    public static void main(String args[])
    {
        OFSetConfig h = new OFSetConfig();
        System.out.println( "Test header: " + h);
    }
}
