package com.fluxtream.connectors.fluxtream_capture;

import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import org.springframework.stereotype.Component;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Component
@Updater(prettyName = "FluxtreamCapture",
         value = 42,
         objectTypes = {FluxtreamCapturePhotoFacet.class},
         defaultChannels = {"FluxtreamCapture.photo"})
public class FluxtreamCaptureUpdater extends AbstractUpdater {
    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        // nothing to do!
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
        // nothing to do!

    }
}
