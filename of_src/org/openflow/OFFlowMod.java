package org.openflow;

class OFFlowMod extends OFMessage
{
    public OFFlowMod()
    {
        super();
        setType(OFPT_FLOW_MOD);
    }
    public static void main(String args[])
    {
        OFFlowMod h = new OFFlowMod();
        System.out.println( "Test header: " + h);
    }
}
