package org.fluxtream.core.connectors.updaters;

/**
 * User: candide
 * Date: 26/08/14
 * Time: 15:08
 */
public class AuthRevokedException extends Exception {

    private final boolean dataCleanupRequested;

    public AuthRevokedException(boolean dataCleanupRequested) {
        this.dataCleanupRequested = dataCleanupRequested;
    }

    public boolean isDataCleanupRequested() {
        return dataCleanupRequested;
    }

}
