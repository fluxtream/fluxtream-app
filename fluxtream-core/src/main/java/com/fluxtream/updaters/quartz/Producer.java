package com.fluxtream.updaters.quartz;

import org.apache.log4j.Logger;


public class Producer {

	static Logger logger = Logger.getLogger(Producer.class);
	
	public void scheduleIncrementalUpdates() {
		logger.debug("shall we schedule an incremental update job?");
	}
	
}
