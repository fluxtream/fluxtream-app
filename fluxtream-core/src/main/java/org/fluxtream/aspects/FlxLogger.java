package org.fluxtream.aspects;

import com.newrelic.api.agent.NewRelic;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * User: candide
 * Date: 12/03/13
 * Time: 14:41
 */
public class FlxLogger {

    Logger logger;

    FlxLogger(Logger logger) {
        this.logger = logger;
    }

    public static final FlxLogger getLogger(Class clazz) {
        return new FlxLogger(Logger.getLogger(clazz));
    }

    public static final FlxLogger getLogger(String name) {
        return new FlxLogger(Logger.getLogger(name));
    }

    public void info(StringBuilder message) {
        logger.info(message);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void error(StringBuilder message) {
        logger.error(message);
        NewRelic.noticeError("ERROR: " + message);
    }

    public void error(String message) {
        logger.error(message);
        NewRelic.noticeError("ERROR: " + message);
    }

    public void warn(StringBuilder message) {
        logger.warn(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void debug(final String message) {
        logger.debug(message);
    }

    public void error(final String message, final Exception e) {
        logger.error(message, e);
        NewRelic.noticeError(e);
    }

    public boolean isEnabledFor(final Level level) {
        return logger.isEnabledFor(level);
    }
}
