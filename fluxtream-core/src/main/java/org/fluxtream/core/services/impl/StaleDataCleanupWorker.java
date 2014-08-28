package org.fluxtream.core.services.impl;

import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 13/04/13
 * Time: 10:59
 */
@Component
@Scope("prototype")
public class StaleDataCleanupWorker implements Runnable {

    FlxLogger logger = FlxLogger.getLogger(StaleDataCleanupWorker.class);

    @Autowired
    ApiDataCleanupService apiDataCleanupService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Override
    public void run() {
        try {
            apiDataCleanupService.cleanupStaleData();
            connectorUpdateService.cleanupStaleData();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=StaleDataCleanupWorker action=cleanupStaleData")
                    .append(" message=\"" + e.getMessage() + "\"");
            logger.warn(sb.toString());
        }
    }
}
