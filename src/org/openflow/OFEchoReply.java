package org.openflow;

class OFEchoReply extends OFMessage
{
    public OFEchoReply()
    {
        super();
        setType(OFPT_ECHO_REPLY);
    }
    public static void main(String args[])
    {
        OFEchoReply h = new OFEchoReply();
        System.out.println( "Test header: " + h);
    }
}
