package glacier.openpath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.TwoLeggedOAuthHelper;
import org.fluxtream.connectors.annotations.Updater;
import org.fluxtream.connectors.location.LocationFacet;
import org.fluxtream.connectors.updaters.AbstractUpdater;
import org.fluxtream.connectors.updaters.UpdateInfo;

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

}
