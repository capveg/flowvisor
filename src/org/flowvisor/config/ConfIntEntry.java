package org.flowvisor.config;

public class ConfIntEntry extends ConfigEntry {
	
	public ConfIntEntry(String name) {
		super(name, ConfigType.INT);
	}

	int val;
	
	/** 
	 * Get the node's value
	 * @return
	 */
	public int getInt() {
		return this.val;
	}
	
	/** 
	 * Set the node's value
	 * @param val
	 */
	public void setInt(int val) {
		this.val = val;
	}
	
	/**
	 * Convert val to an integer and store it
	 * @param val e.g., "12345"
	 */
	@Override
	public void setValue(String val) {
		this.val = Integer.decode(val);
	}
	@Override
	/**
	 * @return an string representation of value
	 */
	public String getValue() {
		return Integer.toString(this.val);
	}
}
