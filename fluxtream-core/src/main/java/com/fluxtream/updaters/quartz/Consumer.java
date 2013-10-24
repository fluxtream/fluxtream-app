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

    private boolean contextStarted = false;

    public void setContextStarted() {
        contextStarted = true;
    }

	public void checkUpdatesQueue() throws Exception {
        while (!contextStarted) {
            Thread.sleep(1000);
            System.out.println("Context not started, delaying queue consumption...");
        }
        logger.debug("module=updateQueue component=consumer action=checkUpdatesQueue");
        connectorUpdateService.pollScheduledUpdateWorkerTasks();
	}


}
