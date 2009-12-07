package org.openflow;

class OFGetConfigRequest extends OFMessage
{
    public OFGetConfigRequest()
    {
        super(OF_HEADER_SIZE);
        setType(OFPT_GET_CONFIG_REQUEST);
    }
    public static void main(String args[])
    {
        OFGetConfigRequest h = new OFGetConfigRequest();
        System.out.println( "Test header: " + h);
    }
}
