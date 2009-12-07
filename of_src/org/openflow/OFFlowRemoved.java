package org.openflow;

class OFFlowRemoved extends OFMessage
{
	public static final int OF_FLOWREMOVED_LENGTH = 88;
    public OFFlowRemoved()
    {
        super(OF_FLOWREMOVED_LENGTH);
        setType(OFPT_FLOW_REMOVED);
    }
    public static void main(String args[])
    {
        OFFlowRemoved h = new OFFlowRemoved();
        System.out.println( "Test header: " + h);
    }
}
