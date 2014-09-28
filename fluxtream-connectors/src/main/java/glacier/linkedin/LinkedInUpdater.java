package glacier.linkedin;

import org.springframework.stereotype.Component;

import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "linkedin", value = 24, objectTypes ={})
public class LinkedInUpdater extends AbstractUpdater {

	public LinkedInUpdater() {
		super();
	}

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        throw new RuntimeException("Not Yet Implemented");
    }

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
	}

}
