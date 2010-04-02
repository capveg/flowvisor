/**
 * 
 */
package org.flowvisor.config;

/**
 * @author capveg
 *
 */
public enum ConfigType {
	
	DIR(ConfDirEntry.class),		// directory
	STR(ConfigEntry.class),		// string
	INT(ConfIntEntry.class),		// integer
	REAL(ConfigEntry.class),		// real
	FLOWMAP(ConfigEntry.class);		// flowmap

	protected Class<? extends ConfigEntry> clazz;

	
	ConfigType(Class<? extends ConfigEntry> clazz) {
		this.clazz = clazz;
	}
	
	public Class<? extends ConfigEntry> toClass() {
		return this.clazz;
	}
}
