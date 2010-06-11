package org.flowvisor.flows;

import org.flowvisor.config.DefaultConfig;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

import junit.framework.TestCase;

public class FlowEntryTest extends TestCase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		DefaultConfig.init("blah"); // setup a default config
	}

	public void testFlowMatch() {
		OFMatch allmatch = new OFMatch();
		allmatch.setWildcards(OFMatch.OFPFW_ALL);
		OFMatch goodmatch = new OFMatch();
		goodmatch.fromString("nw_proto=8");

		OFMatch badmatch = new OFMatch();
		badmatch.fromString("nw_proto=3");

		// match is an XXXX of rule
		FlowEntry flowEntryAll = new FlowEntry(1, allmatch, (OFAction) null);
		TestCase.assertEquals(MatchType.EQUAL, flowEntryAll
				.matches(1, allmatch).getMatchType());
		TestCase.assertEquals(MatchType.SUBSET, flowEntryAll.matches(1,
				goodmatch).getMatchType());
		TestCase.assertEquals(MatchType.SUBSET, flowEntryAll.matches(1,
				badmatch).getMatchType());

		FlowEntry flowEntrySpecific = new FlowEntry(1, goodmatch,
				(OFAction) null);
		TestCase.assertEquals(MatchType.SUPERSET, flowEntrySpecific.matches(1,
				allmatch).getMatchType());
		TestCase.assertEquals(MatchType.EQUAL, flowEntrySpecific.matches(1,
				goodmatch).getMatchType());
		TestCase.assertEquals(MatchType.NONE, flowEntrySpecific.matches(1,
				badmatch).getMatchType());

	}

	public void testCIDRFlowMatch() {
		OFMatch allmatch = new OFMatch();
		allmatch.setWildcards(OFMatch.OFPFW_ALL);
		OFMatch l32badmatch = new OFMatch();
		l32badmatch.fromString("nw_dst=192.168.255.4");
		OFMatch l32match = new OFMatch();
		l32match.fromString("nw_dst=192.168.6.4");
		OFMatch l24match = new OFMatch();
		l24match.fromString("nw_dst=192.168.6.0/24");
		OFMatch l8match = new OFMatch();
		l8match.fromString("nw_dst=192.0.0.0/8");

		// rule is an {SUBSET, SUPERSET, EQUAL} of match
		FlowEntry flowEntry24 = new FlowEntry(1, l24match, (OFAction) null);
		TestCase.assertEquals(MatchType.SUBSET, flowEntry24
				.matches(1, l32match).getMatchType());
		TestCase.assertEquals(MatchType.EQUAL, flowEntry24.matches(1, l24match)
				.getMatchType());
		TestCase.assertEquals(MatchType.SUPERSET, flowEntry24.matches(1,
				allmatch).getMatchType());
		TestCase.assertEquals(MatchType.SUPERSET, flowEntry24.matches(1,
				l8match).getMatchType());
		TestCase.assertEquals(MatchType.NONE, flowEntry24.matches(1,
				l32badmatch).getMatchType());
	}

	public void testDL_VLAN_PCP() {
		OFMatch vpcp = new OFMatch();
		vpcp.setWildcards(~OFMatch.OFPFW_DL_VLAN_PCP);
		vpcp.setDataLayerVirtualLanPriorityCodePoint((byte) 3);

		OFMatch vpcp2 = new OFMatch();
		vpcp2.fromString("dl_vpcp=3");

		FlowEntry flowEntry = new FlowEntry(1, vpcp, (OFAction) null);
		TestCase.assertEquals(MatchType.EQUAL, flowEntry.matches(1, vpcp)
				.getMatchType());
		TestCase.assertEquals(MatchType.EQUAL, flowEntry.matches(1, vpcp2)
				.getMatchType());

	}
}
