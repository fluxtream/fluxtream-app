package glacier.openpath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.TwoLeggedOAuthHelper;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;

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
