package org.openflow;

class OFError extends OFMessage
{
    public OFError()
    {
        super();
        setType(OFPT_ERROR);
    }
    public static void main(String args[])
    {
        OFError h = new OFError();
        System.out.println( "Test header: " + h);
    }
}
