package com.fluxtream.connectors.zeo;

import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.updaters.UpdateInfo.UpdateType;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.HttpUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Component
@Controller
@Updater(prettyName = "Zeo", value = 3, updateStrategyType = UpdateStrategyType.INCREMENTAL,
	objectTypes = { ZeoSleepStatsFacet.class }, extractor = ZeoSleepStatsFacetExtractor.class,
    defaultChannels = {"Zeo.Sleep_Graph"})
@JsonFacetCollection(ZeoFacetVOCollection.class)
public class ZeoRestUpdater extends AbstractUpdater {

	Logger logger = Logger.getLogger(ZeoRestUpdater.class);

    @Qualifier("metadataServiceImpl")
    @Autowired
	MetadataService metadataService;

	private static final DateTimeFormatter formatter = DateTimeFormat
			.forPattern("yyyy-MM-dd");

	public ZeoRestUpdater() {
		super();
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
		getBulkSleepRecordsSinceDate(updateInfo, null);
	}

	@Override
	protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		ApiUpdate lastSuccessfulUpdate = connectorUpdateService
				.getLastSuccessfulUpdate(updateInfo.getGuestId(),
						Connector.getConnector("zeo"));

        DateTime date = new DateTime(lastSuccessfulUpdate.ts);

		getBulkSleepRecordsSinceDate(updateInfo, date);
	}

	@RequestMapping(value = "/zeo/{guestId}/notify")
	public void notifyMeasurement(@PathVariable final Long guestId) {

		connectorUpdateService.addApiNotification(
				Connector.getConnector("zeo"), guestId, "sleep data uploaded");

		logger.info("action=apiNotification connector=zeo message=sleepDataUploaded");
		
		long now = System.currentTimeMillis();
		
		ApiUpdate lastSuccessfulUpdate = connectorUpdateService
				.getLastSuccessfulUpdate(guestId, Connector.getConnector("zeo"));

		UpdateType updateType = lastSuccessfulUpdate != null ? UpdateType.PUSH_TRIGGERED_UPDATE
				: UpdateType.INITIAL_HISTORY_UPDATE;

		connectorUpdateService.scheduleUpdate(guestId, "zeo", -1, updateType,
				now);
	}

	private void getBulkSleepRecordsSinceDate(UpdateInfo updateInfo, DateTime d) throws Exception {
		String zeoApiKey = env.get("zeoApiKey");

        String date = (d==null)?"":d.toString(formatter);

		long then = System.currentTimeMillis();
		String bulkUrl = "http://api.myzeo.com:8080/zeows/api/v1/json/"
				+ "sleeperService/getBulkSleepRecordsSinceDate?key="
				+ zeoApiKey + "&userid=" + updateInfo.getGuestId() + "&date=" +
                date;
		String bulkResult;
		try {
			bulkResult = HttpUtils.fetch(bulkUrl, env,
					env.get("zeoDeveloperUsername"),
					env.get("zeoDeveloperPassword"));
		} catch (Throwable t) {
			t.printStackTrace();
			countFailedApiCall(updateInfo.getGuestId(), -1, then, bulkUrl);
			throw new Exception(t);
		}
		countSuccessfulApiCall(updateInfo.getGuestId(), -1, then, bulkUrl);

		apiDataService.cacheApiDataJSON(updateInfo, bulkResult, -1, -1);
	}

}
