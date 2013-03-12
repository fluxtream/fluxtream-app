package com.fluxtream.updaters.quartz;

import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.aspects.FlxLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Consumer {
	
	@Autowired
	ConnectorUpdateService connectorUpdateService;
	
	static FlxLogger logger = FlxLogger.getLogger(Consumer.class);

	public void checkUpdatesQueue() throws Exception {
        logger.debug("module=updateQueue component=consumer action=checkUpdatesQueue");
		connectorUpdateService.pollScheduledUpdates();
	}


}
