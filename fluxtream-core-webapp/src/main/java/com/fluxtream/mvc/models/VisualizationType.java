package com.fluxtream.mvc.models;

public enum VisualizationType {
	
	CLOCK, STATS, LIST, TIMELINE, TOOLS;

	public static VisualizationType fromValue(String value) {
		return Enum.valueOf(VisualizationType.class, value);
	}
	
}
