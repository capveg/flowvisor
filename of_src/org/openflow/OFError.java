package org.openflow;

class OFError extends OFMessage
{
	public static int DEFAULT_CAPACITY = 4096;
    public OFError()
    {
        super(DEFAULT_CAPACITY);
        setType(OFPT_ERROR);
    }
    public static void main(String args[])
    {
        OFError h = new OFError();
        System.out.println( "Test header: " + h);
    }
}
