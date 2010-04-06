/**
 * 
 */
package org.flowvisor.config;

/**
 * @author capveg
 *
 */
public class ConfStrEntry extends ConfigEntry {
	String val;
	public ConfStrEntry(String name) {
		super(name, ConfigType.STR);
	}
	
	@Override
	public String getValue() {
		return val;	
	}
	
	@Override
	public void setValue(String val) {
		this.val = val;
	}
}
