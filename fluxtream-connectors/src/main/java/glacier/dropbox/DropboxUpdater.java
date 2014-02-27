package glacier.dropbox;

import org.springframework.stereotype.Component;

import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;

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
