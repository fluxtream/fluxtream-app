package org.fluxtream.connectors.zeo;

import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Connector.UpdateStrategyType;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.services.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

@Component
@Controller
@Updater(prettyName = "Zeo", value = 3, updateStrategyType = UpdateStrategyType.INCREMENTAL,
	objectTypes = { ZeoSleepStatsFacet.class }, extractor = ZeoSleepStatsFacetExtractor.class,
    defaultChannels = {"Zeo.Sleep_Graph"})
public class ZeoRestUpdater extends AbstractUpdater {

	FlxLogger logger = FlxLogger.getLogger(ZeoRestUpdater.class);

    @Qualifier("metadataServiceImpl")
    @Autowired
	MetadataService metadataService;

    @Autowired
    JPADaoService jpaDaoService;


	public ZeoRestUpdater() {
		super();
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
        // As of May 30, 2013 Zeo's servers are no longer responding so updating no longer works.
        // Just skip outat this point to avoid gratuitous errors on accounts which have
        // Zeo connectors

		//getBulkSleepRecordsSinceDate(updateInfo, null);
	}

	@Override
	protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		        // As of May 30, 2013 Zeo's servers are no longer responding so updating no longer works.
        // Just skip outat this point to avoid gratuitous errors on accounts which have
        // Zeo connectors

		//ZeoSleepStatsFacet lastFacet = jpaDaoService.findOne("zeo.sleep.getNewest",
        //                                                ZeoSleepStatsFacet.class, updateInfo.getGuestId());
        //
        //DateTime date = new DateTime(lastFacet.end);
        //
        //getBulkSleepRecordsSinceDate(updateInfo, date);
	}

    //private void getBulkSleepRecordsSinceDate(UpdateInfo updateInfo, DateTime d) throws Exception {
		//String zeoApiKey = guestService.getApiKeyAttribute(updateInfo.apiKey, "zeoApiKey");
		//long then = System.currentTimeMillis();
		//String baseUrl = "http://api.myzeo.com:8080/zeows/api/v1/json/sleeperService/";
    //
    //    String date = (d==null)?"":("&dateFrom=" + d.toString(formatter));
    //    String datesUrl = baseUrl + "getDatesWithSleepDataInRange?key=" + zeoApiKey + date;
    //
    //    String username = guestService.getApiKeyAttribute(updateInfo.apiKey, "username");
    //    String password = guestService.getApiKeyAttribute(updateInfo.apiKey, "password");
    //
    //    String days;
		//try {
		//	days = callURL(datesUrl, username, password);
    //        countSuccessfulApiCall(updateInfo.apiKey, -1, then, datesUrl);
    //    } catch (IOException e) {
    //        countFailedApiCall(updateInfo.apiKey, -1, then, datesUrl, Utils.stackTrace(e));
    //        throw e;
    //    }
    //    JSONObject dateList = JSONObject.fromObject(days).getJSONObject("response").optJSONObject("dateList");
    //    if(dateList != null)
    //    {
    //        JSONArray dates = null;
    //        final JSONObject dateJsonObject = dateList.optJSONObject("date");
    //        if (dateJsonObject!=null) {
    //            dates = new JSONArray();
    //            dates.add(dateJsonObject);
    //        } else {
    //            dates = dateList.optJSONArray("date");
    //        }
    //        if(dates != null)
    //        {
    //            String statsUrl = baseUrl + "getSleepRecordForDate?key=" + zeoApiKey + "&date=";
    //            for(Object o : dates)
    //            {
    //                JSONObject json = (JSONObject) o;
    //                int year = json.getInt("year");
    //                int month = json.getInt("month");
    //                int day = json.getInt("day");
    //                String finalStatsUrl = statsUrl + year + "-" + month + "-" + day;
    //                try{
    //                    then = System.currentTimeMillis();
    //                    String bulkResult = callURL(finalStatsUrl, username, password);
    //                    countSuccessfulApiCall(updateInfo.apiKey, -1, then, statsUrl);
    //                    apiDataService.cacheApiDataJSON(updateInfo, bulkResult, -1, -1);
    //                }
    //                catch (IOException e)
    //                {
    //                    countFailedApiCall(updateInfo.apiKey, -1, then, statsUrl, Utils.stackTrace(e));
    //                    throw e;
    //                }
    //            }
    //        }
    //    }
    //
    //}
    //
    ///**
    // * Calls the url after adding authentication information that the user provided when the connector was added
    // * Based on code samples provided by zeo.
    // * @param url_address The url to call
    // * @param username the guest's Zeo username
    // * @param password the guest's Zeo password
    // * @return the result provided by the zeo api
    // * @throws IOException If a URL is malformed, or connection to the zeo api services could not be created
    // */
    //public static String callURL(String url_address, String username, String password) throws IOException {
    //
    //    URL url = new URL(url_address);
    //    URLConnection connection = url.openConnection();
    //
    //    String usernameAndPassword = username + ":" + password;
    //    String encodedAuth = Base64.encodeBase64String(usernameAndPassword.getBytes());
    //
    //    connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
    //    connection.addRequestProperty("Referer", "fluxtream.com");
    //    connection.addRequestProperty("Accept", "application/json");
    //
    //    String line;
    //
    //    StringBuilder builder = new StringBuilder();
    //    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    //
    //    while((line = reader.readLine()) != null) {
    //         builder.append(line);
    //    }
    //
    //    return builder.toString();
    //
    //}

}
