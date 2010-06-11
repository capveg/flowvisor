package org.flowvisor.config;

import org.flowvisor.config.FVConfig;

import junit.framework.TestCase;

public class FVConfigTest extends TestCase {
	public void testSanitize() throws Exception {
		String unquoted = "hi!foo!bar";
		String correct = "hi_foo_bar";
		String quoted = FVConfig.sanitize(unquoted);

		TestCase.assertEquals(correct, quoted);
	}
}
