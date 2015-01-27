package glacier.foursquare;

import org.fluxtream.core.domain.ApiKey;
import org.springframework.stereotype.Component;

import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
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

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {}

}
