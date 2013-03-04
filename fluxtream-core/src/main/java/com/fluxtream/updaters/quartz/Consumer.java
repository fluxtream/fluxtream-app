package com.fluxtream.updaters.quartz;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.services.ConnectorUpdateService;

@Component
public class Consumer implements InitializingBean {
	
	@Autowired
	ConnectorUpdateService connectorUpdateService;
	
	static Logger logger = Logger.getLogger(Consumer.class);

	public void checkUpdatesQueue() throws Exception {
        logger.debug("module=updateQueue component=consumer action=checkUpdatesQueue");
		connectorUpdateService.pollScheduledUpdates();
	}

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("THIS METHOD IS EXECUTING FIRST");
        connectorUpdateService.resumeInterruptedUpdates();
    }

}
