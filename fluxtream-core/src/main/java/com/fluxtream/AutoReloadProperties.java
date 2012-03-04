package com.fluxtream;

import java.io.InputStream;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

public class AutoReloadProperties extends PropertiesConfiguration {

	public AutoReloadProperties(String propertyFile) throws ConfigurationException {
		setReloadingStrategy(new FileChangedReloadingStrategy());
		InputStream stream = getClass().getClassLoader().getResourceAsStream(propertyFile);
		this.load(stream);
	}
	
	public String get(String key) {
		return (String) getProperty(key);
	}
	
}
