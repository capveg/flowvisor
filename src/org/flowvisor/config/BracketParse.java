package org.flowvisor.config;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Takes a string of the form "foo[key1=[val1],key2=[val2],]" are returns a hashmap containg
 * ( key1 			=> val1, 
 *   key2 			=> val2,
 *   "ObjectName" 	=> foo)
 *   
 *   Critically, values are untouched, i.e., if they contain more square brackets and commas,
 *   	they are unaffected
 * @author capveg
 *
 */

public class BracketParse {
	final public static String OBJECTNAME="ObjectName";
	/**
	 * Parse a BacketParse encoded line
	 * 
	 * @param line
	 * @return null if unparsed, else a hashmap, as above
	 */
	
	static HashMap<String,String> decode(String line) {
		HashMap<String,String> map = new LinkedHashMap<String,String>();
		int index = line.indexOf("[");
		if (index< 0)
			return null;	// unparsed
		
		
		return map;
	}
}
