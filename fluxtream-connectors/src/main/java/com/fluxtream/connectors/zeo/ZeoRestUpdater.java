package com.fluxtream.connectors.zeo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.updaters.UpdateInfo.UpdateType;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.MetadataService;
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
import sun.misc.BASE64Encoder;

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

    @Autowired
    JPADaoService jpaDaoService;

    private BASE64Encoder enc = new BASE64Encoder();

	private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

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
		ZeoSleepStatsFacet lastFacet = jpaDaoService.findOne("zeo.sleep.getNewest",
                                                        ZeoSleepStatsFacet.class, updateInfo.getGuestId());

        DateTime date = new DateTime(lastFacet.end);

		getBulkSleepRecordsSinceDate(updateInfo, date);
	}

    @Deprecated
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

        String date = (d==null)?"":("&dateFrom=" + d.toString(formatter));

		long then = System.currentTimeMillis();
		String bulkUrl = "http://api.myzeo.com:8080/zeows/api/v1/json/"
				+ "sleeperService/getDatesWithSleepDataInRange?key="
				+ zeoApiKey + date;
		String bulkResult;
		try {
			bulkResult = callURL(updateInfo.getGuestId(), bulkUrl);
		} catch (IOException e) {
			countFailedApiCall(updateInfo.getGuestId(), -1, then, bulkUrl);
			throw e;
		}
		countSuccessfulApiCall(updateInfo.getGuestId(), -1, then, bulkUrl);

		apiDataService.cacheApiDataJSON(updateInfo, bulkResult, -1, -1);
	}

    /**
     * Calls the url after adding authentication information that the user provided when the connector was added
     * @param guestId The user of this connector
     * @param url_address The url to call
     * @return the result provided by the zeo api
     * @throws IOException If a URL is malformed, or connection to the zeo api services could not be created
     */
    private String callURL(long guestId, String url_address) throws IOException {

        URL url = new URL(url_address);
        URLConnection connection = url.openConnection();

        String username = guestService.getApiKeyAttribute(guestId, connector(), "username");
        String password = guestService.getApiKeyAttribute(guestId, connector(), "password");
        String usernameAndPassword = username + ":" + password;
        String encodedAuth = enc.encode(usernameAndPassword.getBytes());

        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
        connection.addRequestProperty("Referer", "fluxtream.com");
        connection.addRequestProperty("Accept", "application/json");

        String line;

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        while((line = reader.readLine()) != null) {
             builder.append(line);
        }

        return builder.toString();

    }

}
