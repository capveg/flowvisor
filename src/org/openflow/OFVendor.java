package org.openflow;

class OFVendor extends OFMessage
{
    public OFVendor()
    {
        super();
        setType(OFPT_VENDOR);
    }
    public static void main(String args[])
    {
        OFVendor h = new OFVendor();
        System.out.println( "Test header: " + h);
    }
}
