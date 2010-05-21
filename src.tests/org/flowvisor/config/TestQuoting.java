package org.flowvisor.config;

import org.flowvisor.config.FVConfig;

import junit.framework.TestCase;

public class TestQuoting extends TestCase {
	public void testQuoting() throws Exception {
		String unquoted = "hi.foo.bar";
		String correct =  "hi\\.foo\\.bar";
		String quoted = FVConfig.quote(unquoted);
		
		TestCase.assertEquals(correct,quoted);
		TestCase.assertEquals(unquoted, FVConfig.unquote(quoted));
	}
}
