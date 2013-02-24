package com.fluxtream.updaters.quartz;

import com.fluxtream.services.ConnectorUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class Cleanup {

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    public void resumeInterruptedUpdates() {
        connectorUpdateService.resumeInterruptedUpdates();
    }

}
