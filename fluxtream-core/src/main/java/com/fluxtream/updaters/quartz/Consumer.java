package com.fluxtream.updaters.quartz;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.services.ConnectorUpdateService;

@Component
public class Consumer {
	
	@Autowired
	ConnectorUpdateService connectorUpdateService;
	
	static Logger logger = Logger.getLogger(Consumer.class);

	public void checkUpdatesQueue() throws Exception {
        logger.debug("module=updateQueue component=consumer action=checkUpdatesQueue");
		connectorUpdateService.pollScheduledUpdates();
	}
	

		
}
