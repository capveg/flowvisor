package org.flowvisor.flows;

import java.util.Set;

import junit.framework.TestCase;

import org.flowvisor.config.DefaultConfig;
import org.flowvisor.config.FVConfig;

public class FlowSpaceUtilsTest extends TestCase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// load default config
		DefaultConfig.init("0fw0rk");
	}

	public void testByDPID() {
		long dpid = 1;
		Set<String> slices = FlowSpaceUtil.getSlicesByDPID(FVConfig
				.getFlowSpaceFlowMap(), dpid);
		TestCase.assertEquals(2, slices.size());
		TestCase.assertTrue(slices.contains("alice"));
		TestCase.assertTrue(slices.contains("bob"));
	}

	public void testByPort() {
		long dpid = 1;
		Set<Short> ports = FlowSpaceUtil.getPortsBySlice(dpid, "alice");
		TestCase.assertEquals(3, ports.size());
		TestCase.assertTrue(ports.contains(Short.valueOf((short) 0)));
		TestCase.assertTrue(ports.contains(Short.valueOf((short) 2)));
		TestCase.assertTrue(ports.contains(Short.valueOf((short) 3)));
	}

	public void testGetSubFlowSpace() {
		long dpid = 57;
		FlowMap flowMap = FlowSpaceUtil.getSubFlowMap(dpid);
		TestCase.assertEquals(10, flowMap.countRules());
		for (FlowEntry flowEntry : flowMap.getRules())
			TestCase.assertEquals(dpid, flowEntry.getDpid());
	}
}
