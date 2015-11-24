package org.fluxtream.connectors.google_spreadsheets;

import org.fluxtream.core.connectors.updaters.UpdateInfo;

/**
 * Created by candide on 12/11/15.
 */
public interface GoogleSpreadsheetsDao {
    void removeDocument(long id);

    boolean isDupe(UpdateInfo updateInfo, GoogleSpreadsheetsUpdater.ImportSpecs importSpecs);
}
