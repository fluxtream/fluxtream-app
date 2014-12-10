package glacier.openpath;

import org.fluxtream.core.domain.ApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.TwoLeggedOAuthHelper;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;

@Component
@Updater(prettyName = "OpenPath", value = 89, objectTypes = { LocationFacet.class }, hasFacets = true)
public class OpenPathUpdater extends AbstractUpdater {

	@Autowired
	TwoLeggedOAuthHelper twoLeggedOAuthHelper;

	Connector connector;

	public OpenPathUpdater() {
		super();
		connector = Connector.getConnector("openpath");
	}
	public void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
	}

	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {

	}

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {}

}
