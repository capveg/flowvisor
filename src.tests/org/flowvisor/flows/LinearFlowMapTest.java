package org.flowvisor.flows;

import java.util.List;

import junit.framework.TestCase;

import org.openflow.protocol.OFMatch;

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
		TestCase.assertTrue(intersects.get(0).getFlowEntry().matches(1, packet)
				.getMatchType() != MatchType.NONE);
	}
}
