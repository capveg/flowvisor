/**
 * 
 */
package org.flowvisor.config;

/**
 * @author capveg
 *
 */
public class ConfRealEntry extends ConfigEntry {
	double val;
	
	public ConfRealEntry(String name) {
		super(name, ConfigType.REAL);
	}
	
	public double getDouble() {
		return val; 
	}
	
	public void setDouble(double val) {
		this.val = val;
	}
	
	@Override
	public String getValue() {
		return Double.toString(this.val);
	}
	
	@Override 
	public void setValue(String s) {
		this.val = Double.parseDouble(s);
	}
}
