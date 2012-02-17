package com.fluxtream.updaters.strategies;

import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.Connector;

@SuppressWarnings("serial")
public class UnsuitableGranularityException extends RuntimeException {

	Connector api;
	TimeUnit granularity;
	
	public UnsuitableGranularityException(TimeUnit granularity, Connector api) {
		this.granularity = granularity;
		this.api = api;
	}
	
	@Override
	public String getMessage() {
		return api.toString() 
			+ "API data cannot be retrieved "
			+ "at the requested granularity ("
			+ granularity + ")";
	}
	
}
