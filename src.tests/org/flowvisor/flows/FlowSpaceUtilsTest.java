package org.flowvisor.flows;

import java.util.Set;

import junit.framework.TestCase;

import org.flowvisor.config.DefaultConfig;
import org.flowvisor.config.FVConfig;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;

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
		Set<Short> ports = FlowSpaceUtil.getPortsBySlice(dpid, "alice",
				FVConfig.getFlowSpaceFlowMap());
		TestCase.assertEquals(3, ports.size());
		TestCase.assertTrue(ports.contains(Short.valueOf((short) 0)));
		TestCase.assertTrue(ports.contains(Short.valueOf((short) 2)));
		TestCase.assertTrue(ports.contains(Short.valueOf((short) 3)));
	}

	public void testByPortAll() {
		// matches all ports and all dpids
		FlowEntry flowEntry = new FlowEntry(new OFMatch(), new SliceAction(
				"alice", SliceAction.WRITE));
		FlowMap flowMap = new LinearFlowMap();
		flowMap.addRule(flowEntry);
		Set<Short> ports = FlowSpaceUtil.getPortsBySlice(1, "alice", flowMap);
		TestCase.assertEquals(1, ports.size());
		TestCase.assertTrue(ports.contains(OFPort.OFPP_ALL.getValue()));
		// get this subFlowMap with dpid=1
		FlowMap subFlowMap = FlowSpaceUtil.getSubFlowMap(flowMap, 1,
				new OFMatch());
		ports = FlowSpaceUtil.getPortsBySlice(1, "alice", subFlowMap);
		TestCase.assertEquals(1, ports.size());
		TestCase.assertTrue(ports.contains(OFPort.OFPP_ALL.getValue()));

	}

	public void testGetSubFlowSpace() {
		long dpid = 57;
		FlowMap flowMap = FlowSpaceUtil.getSubFlowMap(dpid);
		TestCase.assertEquals(10, flowMap.countRules());
		for (FlowEntry flowEntry : flowMap.getRules())
			TestCase.assertEquals(dpid, flowEntry.getDpid());
	}
}
