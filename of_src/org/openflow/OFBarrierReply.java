package org.openflow;

class OFBarrierReply extends OFMessage
{
    public OFBarrierReply()
    {
        super();
        setType(OFPT_BARRIER_REPLY);
    }
    public static void main(String args[])
    {
        OFBarrierReply h = new OFBarrierReply();
        System.out.println( "Test header: " + h);
    }
}
