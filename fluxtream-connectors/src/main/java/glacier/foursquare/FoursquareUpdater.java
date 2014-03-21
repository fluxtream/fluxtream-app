package glacier.foursquare;

import org.springframework.stereotype.Component;

import org.fluxtream.connectors.annotations.Updater;
import org.fluxtream.connectors.updaters.AbstractUpdater;
import org.fluxtream.connectors.updaters.UpdateInfo;
/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "Foursquare", value = 20, objectTypes ={})
public class FoursquareUpdater extends AbstractUpdater {

	public FoursquareUpdater() {
		super();
	}

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        throw new RuntimeException("Not Yet Implemented");
    }

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) {
	}

}
