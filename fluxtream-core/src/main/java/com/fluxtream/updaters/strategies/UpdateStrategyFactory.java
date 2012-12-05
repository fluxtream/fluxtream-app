package com.fluxtream.updaters.strategies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.connectors.Connector;

@Component
public class UpdateStrategyFactory {

	@Autowired
	private AlwaysUpdateStrategy alwaysUpdateStrategy;
	
	@Autowired
	private IncrementalUpdateStrategy incrementalUpdateStrategy;

	public UpdateStrategy getUpdateStrategy(Connector api) {
		switch (api.updateStrategyType()) {
		case ALWAYS_UPDATE:
			return alwaysUpdateStrategy;
		case INCREMENTAL:
			return incrementalUpdateStrategy;
		}
		return null;
	}
	
}
