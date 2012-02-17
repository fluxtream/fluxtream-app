package com.fluxtream.mvc.models;

public class StatusModel {

	public String result;
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
