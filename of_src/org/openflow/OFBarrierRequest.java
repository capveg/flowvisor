package org.openflow;

class OFBarrierRequest extends OFMessage
{
    public OFBarrierRequest()
    {
        super(OF_HEADER_SIZE);
        setType(OFPT_BARRIER_REQUEST);
    }
    public static void main(String args[])
    {
        OFBarrierRequest h = new OFBarrierRequest();
        System.out.println( "Test header: " + h);
    }
}
