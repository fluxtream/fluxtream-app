package glacier.nikeplus;

import org.springframework.stereotype.Component;

import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "nikeplus", value = 25, objectTypes ={})
public class NikePlusUpdater extends AbstractUpdater {

	public NikePlusUpdater() {
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
