package glacier.khanacademy;

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
@Updater(prettyName = "khanacademy", value = 23, objectTypes ={})
public class KhanAcademyUpdater extends AbstractUpdater {

	public KhanAcademyUpdater() {
		super();
	}

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        throw new RuntimeException("Not Yet Implemented");
    }

    @Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
	}

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {}

}
