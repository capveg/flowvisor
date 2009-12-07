package org.openflow;

class OFVendor extends OFMessage
{
	public static int DEFAULT_CAPACITY = 4096;
    public OFVendor()
    {
        super(DEFAULT_CAPACITY);
        setType(OFPT_VENDOR);
    }
    public static void main(String args[])
    {
        OFVendor h = new OFVendor();
        System.out.println( "Test header: " + h);
    }
}
