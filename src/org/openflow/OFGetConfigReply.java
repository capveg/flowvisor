package org.openflow;

class OFGetConfigReply extends OFMessage
{
    public OFGetConfigReply()
    {
        super();
        setType(OFPT_GET_CONFIG_REPLY);
    }
    public static void main(String args[])
    {
        OFGetConfigReply h = new OFGetConfigReply();
        System.out.println( "Test header: " + h);
    }
}
