/**
 * 
 */
package org.flowvisor.events;

/**
 * Basic unit of information passed between FV's logical units
 * Derived classes express the specific msg information
 * @author capveg
 *
 */
public class FVEvent {
	private FVEventHandler src, dst;
	
	public FVEvent(FVEventHandler src, FVEventHandler dst) {
		this.src = src;
		this.dst = dst;
	}
	
	/**
	 * Get the sending msg handler (could be null) 
	 * @return
	 */
	public FVEventHandler getSrc() {
		return src;
	}
	
	/**
	 * Set the sending Event handler
	 * @param src could be null
	 */
	public void setSrc(FVEventHandler src) {
		this.src = src;
	}
	
	/**
	 * Get the destination of this message
	 * @return dst dst reference
	 */
	public FVEventHandler getDst() {
		return dst;
	}
	/**
	 * Set the destination Event handler
	 * @param dst
	 */
	public void setDst(FVEventHandler dst) {
		this.dst = dst;
	}
}
