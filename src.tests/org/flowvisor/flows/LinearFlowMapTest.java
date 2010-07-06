package org.flowvisor.flows;

import java.util.List;

import junit.framework.TestCase;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.U16;

public class LinearFlowMapTest extends TestCase {
	LinearFlowMap flowmap;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.flowmap = new LinearFlowMap();
		OFMatch match = new OFMatch();
		match.fromString("nw_src=128.8.0.0/16");
		FlowEntry flowEntry = new FlowEntry(match, new SliceAction("alice",
				SliceAction.WRITE));
		flowmap.addRule(flowEntry);
	}

	public void testIntersectCIDR() {
		FlowMap submap = FlowSpaceUtil.getSubFlowMap(this.flowmap, 1,
				new OFMatch());

		OFMatch packet = new OFMatch();
		packet
				.fromString("OFMatch[in_port=1,dl_dst=00:1c:f0:ed:98:5a,dl_src=00:22:41:fa:73:01,dl_type=2048,dl_vlan=-1,dl_vpcp=0,nw_dst=3.4.4.5,nw_src=128.8.9.9,nw_proto=1,nw_tos=0,tp_dst=0,tp_src=8]");
		List<FlowIntersect> intersects = submap.intersects(1, packet);
		TestCase.assertEquals(1, intersects.size());
		TestCase.assertEquals(MatchType.SUBSET, intersects.get(0)
				.getMatchType());
	}

	/**
	 * this is a junit test of tests-vlan.py: this must pass before that can
	 * pass
	 * 
	 * Make sure that a higher priority FlowEntry with a specific vlan does not
	 * eclipse a match all entry
	 * 
	 */
	public void testPacketLookupOnVlanEclipe() {
		FlowMap flowMap = new LinearFlowMap();
		OFMatch matchVlan = new OFMatch();
		matchVlan.fromString("dl_vlan=512");
		FlowEntry feVlan = new FlowEntry(matchVlan, new SliceAction("alice",
				SliceAction.WRITE));
		feVlan.setPriority(1000);
		flowMap.addRule(feVlan);

		TestCase.assertEquals(512, U16.f(feVlan.getRuleMatch()
				.getDataLayerVirtualLan()));
		FlowEntry feAll = new FlowEntry(new OFMatch(), new SliceAction("bob",
				SliceAction.WRITE));
		feAll.setPriority(500);
		flowMap.addRule(feAll);

		OFMatch packet = new OFMatch();
		packet.fromString("OFMatch[in_port=3,dl_dst=00:00:00:00:00:01,"
				+ "dl_src=00:00:00:00:00:02,dl_type=2048,dl_vlan=-1,"
				+ "dl_vpcp=0,nw_dst=-64.168.1.40,nw_src=-64.168.0.40,"
				+ "nw_proto=-1,nw_tos=0,tp_dst=0,tp_src=0]");
		List<FlowEntry> list = flowMap.matches(1, packet);
		TestCase.assertNotNull(list);
		TestCase.assertEquals(1, list.size());

		OFAction ofAction = list.get(0).getActionsList().get(0);
		TestCase.assertTrue(ofAction instanceof SliceAction);
		SliceAction sliceAction = (SliceAction) ofAction;
		TestCase.assertEquals("bob", sliceAction.getSliceName());

		FlowMap subMap = FlowSpaceUtil.getSubFlowMap(flowMap, 1, new OFMatch());
		list = subMap.matches(1, packet);
		TestCase.assertNotNull(list);
		TestCase.assertEquals(1, list.size());

		ofAction = list.get(0).getActionsList().get(0);
		TestCase.assertTrue(ofAction instanceof SliceAction);
		sliceAction = (SliceAction) ofAction;
		TestCase.assertEquals("bob", sliceAction.getSliceName());

	}
}
