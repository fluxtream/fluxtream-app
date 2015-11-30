package org.fluxtream.core.services.impl;

import org.apache.commons.lang.exception.ExceptionUtils;
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
        // let's not do that for the moment
//        try {
//            apiDataCleanupService.cleanupStaleData();
//        }
//        catch (Exception e) {
//            StringBuilder sb = new StringBuilder("Couldn't cleanup api staled data")
//                    .append(" message=\"" + e.getMessage() + "\"\n" + ExceptionUtils.getStackTrace(e));
//            FlxLogger.getLogger("org.fluxtream.core.updaters.quartz").warn(sb.toString());
//        }
        try {
            connectorUpdateService.cleanupStaleData();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("Couldn't cleanup old update data")
                    .append(" message=\"" + e.getMessage() + "\"\n" + ExceptionUtils.getStackTrace(e));
            FlxLogger.getLogger("org.fluxtream.core.updaters.quartz").warn(sb.toString());
        }
    }
}
