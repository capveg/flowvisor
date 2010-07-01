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
		OFMatch aliceMatch = new OFMatch();
		aliceMatch.fromString("dl_src=00:00:00:00:00:01");
		FlowEntry flowEntry1 = new FlowEntry(aliceMatch, new SliceAction(
				"alice", SliceAction.WRITE));
		flowEntry1.setPriority(1);
		OFMatch bobMatch = new OFMatch();
		bobMatch.fromString("in_port=3,dl_src=00:00:00:00:00:02");
		FlowEntry flowEntry2 = new FlowEntry(bobMatch, new SliceAction("bob",
				SliceAction.WRITE));
		flowEntry2.setPriority(2); // has higher priority over fe1
		FlowMap flowMap = new LinearFlowMap();
		flowMap.addRule(flowEntry1);
		flowMap.addRule(flowEntry2);
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
	/*
	 * TODO: Need to fix!
	 * 
	 * public void testDPID2Str() { long dpid = 0xffffffffffffffffl; String
	 * good_str = "ff:ff:ff:ff:ff:ff:ff:ff"; String test_str =
	 * FlowSpaceUtil.dpidToString(dpid); TestCase.assertEquals(good_str,
	 * test_str); long test_dpid = FlowSpaceUtil.parseDPID(test_str);
	 * TestCase.assertEquals(dpid, test_dpid); }
	 * 
	 * 
	 * 
	 * 
	 * 
	 * public void testDPID2Str2() { long dpid = 0x9fffffffffffaf00l; String
	 * good_str = "9f:ff:ff:ff:ff:ff:af:00"; String test_str =
	 * FlowSpaceUtil.dpidToString(dpid); TestCase.assertEquals(good_str,
	 * test_str); long test_dpid = FlowSpaceUtil.parseDPID(test_str);
	 * TestCase.assertEquals(dpid, test_dpid); }
	 */
}
