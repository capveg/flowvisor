package org.json;

import java.lang.reflect.Type;

import com.google.gson.Gson;

public class JSONParam {

	String valueAsJson;

	protected JSONParam(){
		// For Serialization
	}

	public JSONParam(String value){
		this.valueAsJson = value;
	}

	public <T> T getValue(Gson gson, Type t){
		return gson.fromJson(valueAsJson, t);
	}

}
