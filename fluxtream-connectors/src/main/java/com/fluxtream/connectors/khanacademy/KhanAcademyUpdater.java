package com.fluxtream.connectors.khanacademy;

import org.springframework.stereotype.Component;

import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
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
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
	}

}
