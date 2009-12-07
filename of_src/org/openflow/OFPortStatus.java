package org.openflow;

class OFPortStatus extends OFMessage
{
	public static int DEFAULT_CAPACITY = 4096;
    public OFPortStatus()
    {
        super(DEFAULT_CAPACITY);
        setType(OFPT_PORT_STATUS);
    }
    public static void main(String args[])
    {
        OFPortStatus h = new OFPortStatus();
        System.out.println( "Test header: " + h);
    }
}
