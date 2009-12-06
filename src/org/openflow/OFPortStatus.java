package org.openflow;

class OFPortStatus extends OFMessage
{
    public OFPortStatus()
    {
        super();
        setType(OFPT_PORT_STATUS);
    }
    public static void main(String args[])
    {
        OFPortStatus h = new OFPortStatus();
        System.out.println( "Test header: " + h);
    }
}
