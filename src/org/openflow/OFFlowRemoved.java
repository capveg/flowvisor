package org.openflow;

class OFFlowRemoved extends OFMessage
{
    public OFFlowRemoved()
    {
        super();
        setType(OFPT_FLOW_REMOVED);
    }
    public static void main(String args[])
    {
        OFFlowRemoved h = new OFFlowRemoved();
        System.out.println( "Test header: " + h);
    }
}
