package com.fluxtream.updaters.quartz;

import com.fluxtream.services.ConnectorUpdateService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class Cleanup implements InitializingBean {

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("THIS METHOD IS EXECUTING FIRST");
        connectorUpdateService.resumeInterruptedUpdates();
    }
}
