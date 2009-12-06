package org.openflow;

class OFEchoRequest extends OFMessage
{
    public OFEchoRequest()
    {
        super();
        setType(OFPT_ECHO_REQUEST);
    }
    public static void main(String args[])
    {
        OFEchoRequest h = new OFEchoRequest();
        System.out.println( "Test header: " + h);
    }
}
