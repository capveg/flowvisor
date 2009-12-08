package org.openflow;

public class OFMessageFactory {

    public OFFeaturesRequest createFeaturesRequest() {
        return new OFFeaturesRequest();
    }

    public OFHello createHello() {
        return new OFHello();
    }

    public OFVendor createVendor() {
        return new OFVendor();
    }

    public OFSetConfig createSetConfig() {
        return new OFSetConfig();
    }

    public OFStatsRequest createStatsRequest() {
        return new OFStatsRequest();
    }

    public OFPacketIn createPacketIn() {
        return new OFPacketIn();
    }

    public OFFlowRemoved createFlowRemoved() {
        return new OFFlowRemoved();
    }

    public OFFlowMod createFlowMod() {
        return new OFFlowMod();
    }

    public OFFeaturesReply createFeaturesReply() {
        return new OFFeaturesReply();
    }

    public OFPortStatus createPortStatus() {
        return new OFPortStatus();
    }

    public OFGetConfigReply createGetConfigReply() {
        return new OFGetConfigReply();
    }

    public OFEchoRequest createEchoRequest() {
        return new OFEchoRequest();
    }

    public OFMatch createMatch() {
        return new OFMatch();
    }

    public OFBarrierReply createBarrierReply() {
        return new OFBarrierReply();
    }

    public OFGetConfigRequest createGetConfigRequest() {
        return new OFGetConfigRequest();
    }

    public OFEchoReply createEchoReply() {
        return new OFEchoReply();
    }

    public OFStatsReply createStatsReply() {
        return new OFStatsReply();
    }

    public OFPacketOut createPacketOut() {
        return new OFPacketOut();
    }

    public OFError createError() {
        return new OFError();
    }

    public OFPortMod createPortMod() {
        return new OFPortMod();
    }

    public OFBarrierRequest createBarrierRequest() {
        return new OFBarrierRequest();
    }

}
