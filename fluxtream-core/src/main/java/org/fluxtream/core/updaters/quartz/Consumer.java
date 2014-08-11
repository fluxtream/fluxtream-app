package org.fluxtream.core.updaters.quartz;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Consumer {
	
	@Autowired
	ConnectorUpdateService connectorUpdateService;

    @Autowired
    Configuration env;
	
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
