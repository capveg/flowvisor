/**
 * 
 */
package org.flowvisor.log;

/** 
 * Logging Priority Levels (very similar to syslog)
 * Sorted in descending order of importance. 
 * @author capveg
 *
 */
public enum LogLevel {
	FATAL,		// The world is on fire
	CRIT,		// Will always want to know
	ALERT,		// Will typically want to know
	WARN,		// Might want to know cuz it's possibly bad
	INFO,		// Maybe worth knowing, maybe not -- not bad
	DEBUG		// Only worth knowing if debugging
}
