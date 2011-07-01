package org.json;

import java.lang.reflect.Type;
import com.google.gson.Gson;

public class JSONResult {


	String valueAsJson;

	protected JSONResult(){
		// For Serialization
	}

	public JSONResult(String value){
		this.valueAsJson = value;
	}

	public <T>T getValue(Gson gson, Type type){
		return gson.fromJson(valueAsJson, type);
	}

}
