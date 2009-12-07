package org.openflow;

public class OFFeaturesRequest extends OFMessage
{
    public OFFeaturesRequest()
    {
        super(OF_HEADER_SIZE);
        setType(OFPT_FEATURES_REQUEST);
    }
    public static void main(String args[])
    {
        OFFeaturesRequest h = new OFFeaturesRequest();
        System.out.println( "Test header: " + h);
    }
}
