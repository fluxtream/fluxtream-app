package com.fluxtream.connectors.zeo;

import java.io.IOException;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.updaters.UpdateInfo.UpdateType;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.HttpUtils;

@Component
@Controller
@Updater(prettyName = "Zeo", value = 3, updateStrategyType = UpdateStrategyType.PUSH,
	objectTypes = { ZeoSleepStatsFacet.class }, extractor = ZeoSleepStatsFacetExtractor.class)
@JsonFacetCollection(ZeoFacetVOCollection.class)
public class ZeoRestUpdater extends AbstractUpdater {

	Logger logger = Logger.getLogger(ZeoRestUpdater.class);

	@Autowired
	MetadataService metadataService;

	private static final DateTimeFormatter formatter = DateTimeFormat
			.forPattern("yyyy-MM-dd");

	public ZeoRestUpdater() {
		super();
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws RateLimitReachedException, Exception {
		getBulkSleepRecordsSinceDate(updateInfo, "1970-1-1");
	}

	@Override
	protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		ApiUpdate lastSuccessfulUpdate = connectorUpdateService
				.getLastSuccessfulUpdate(updateInfo.getGuestId(),
						Connector.getConnector("zeo"));

		TimeZone currentTimeZone = metadataService
				.getCurrentTimeZone(updateInfo.getGuestId());
		
		String dateOfLastSuccessfullUpdate = formatter.withZone(
				DateTimeZone.forTimeZone(currentTimeZone)).print(
				lastSuccessfulUpdate.ts);

		getBulkSleepRecordsSinceDate(updateInfo,
				dateOfLastSuccessfullUpdate);
	}

	@RequestMapping(value = "/zeo/{guestId}/notify")
	public void notifyMeasurement(@PathVariable final Long guestId,
			HttpServletRequest request, HttpServletResponse response) {

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

	private void getBulkSleepRecordsSinceDate(UpdateInfo updateInfo, String date)
			throws HttpException, IOException, Exception {
		String zeoApiKey = env.get("zeoApiKey");

		long then = System.currentTimeMillis();
		String bulkUrl = "http://api.myzeo.com:8080/zeows/api/v1/json/"
				+ "sleeperService/getBulkSleepRecordsSinceDate?key="
				+ zeoApiKey + "&userid=" + updateInfo.getGuestId() + "&date=";
		String bulkResult = "";
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
