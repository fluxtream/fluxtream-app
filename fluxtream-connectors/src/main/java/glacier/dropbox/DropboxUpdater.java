package glacier.dropbox;

import org.springframework.stereotype.Component;

import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;

/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "Dropbox", value = 35, objectTypes ={})
public class DropboxUpdater extends AbstractUpdater {

	public DropboxUpdater() {
		super();
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) {
	}
	
	@Override
	public void updateConnectorDataHistory(UpdateInfo updateInfo) {
		
	}
	

}
