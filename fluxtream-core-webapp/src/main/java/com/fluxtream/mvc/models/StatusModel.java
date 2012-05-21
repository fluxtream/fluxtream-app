package com.fluxtream.mvc.models;

import com.google.gson.annotations.Expose;

public class StatusModel {

    @Expose
	public String result;
    @Expose
	public String message;
	public String stackTrace;
	public Object payload;
	
	public StatusModel(boolean b, String message) {
		result = b?"OK":"KO";
		this.message = message;
	}
	
	public StatusModel(boolean b, String message, String stackTrace) {
		result = b?"OK":"KO";
		this.message = message;
		this.stackTrace = stackTrace;
	}
	
}
