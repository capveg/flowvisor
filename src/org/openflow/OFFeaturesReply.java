package org.openflow;

class OFFeaturesReply extends OFMessage
{
    public OFFeaturesReply()
    {
        super();
        setType(OFPT_FEATURES_REPLY);
    }
    public static void main(String args[])
    {
        OFFeaturesReply h = new OFFeaturesReply();
        System.out.println( "Test header: " + h);
    }
}
