/**
 *
 */
package org.flowvisor.classifier;

import org.flowvisor.slicer.FVSlicer;

/**
 * @author capveg
 *
 */
public class XidPair {
	int xid;
	FVSlicer fvSlicer;

	public XidPair(int xid, FVSlicer fvSlicer) {
		this.xid = xid;
		this.fvSlicer = fvSlicer;
	}

	public int getXid() {
		return xid;
	}

	public void setXid(int xid) {
		this.xid = xid;
	}

	public FVSlicer getFvSlicer() {
		return fvSlicer;
	}

	public void setFvSlicer(FVSlicer fvSlicer) {
		this.fvSlicer = fvSlicer;
	}
}
