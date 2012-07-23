package com.fluxtream.connectors.quantifiedmind;

import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import org.springframework.stereotype.Component;

/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "quantifiedmind", value = 100, objectTypes ={})
public class QuantifiedMindUpdater extends AbstractUpdater {

	public QuantifiedMindUpdater() {
		super();
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
	}

}
