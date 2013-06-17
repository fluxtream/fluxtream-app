package com.fluxtream.connectors.moves;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 16:50
 */
@Component
@Updater(prettyName = "Moves", value = 144, objectTypes = {LocationFacet.class}, extractor = MovesFacetExtractor.class)
public class MovesUpdater extends AbstractUpdater {

    final static String host = "https://api.moves-app.com/api/v1";
    public static DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyyMMdd");

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        long then = System.currentTimeMillis();
        String query = host + "/user/profile";
        String firstDate = null;
        try {
            final String fetched = HttpUtils.fetch(query);
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query);
            JSONObject json = JSONObject.fromObject(fetched);
            if (!json.has("profile"))
                throw new Exception("no profile");
            final JSONObject profile = json.getJSONObject("profile");
            if (!profile.has("firstDate"))
                throw new Exception("no firstDate in profile");
            firstDate = profile.getString("firstDate");
        } catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query, Utils.stackTrace(e));
        }

        updateConnectorDataSince(updateInfo, firstDate);
    }

    private void updateConnectorDataSince(final UpdateInfo updateInfo, final String lastDate) {
        List<String> dates = getDatesSince(lastDate);
        for (String date : dates) {
            long then = System.currentTimeMillis();
            try {
                String fetchUrl = String.format(host + "/user/storyline/daily/%s?trackPoints=true",date);
                final String fetched = HttpUtils.fetch(fetchUrl);
                countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, fetchUrl);
                apiDataService.cacheApiDataJSON(updateInfo, fetched, -1, -1);
                guestService.setApiKeyAttribute(updateInfo.apiKey, "lastDate", date);
            } catch (Exception e) {
                countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, date, Utils.stackTrace(e));
            }
        }
    }

    private static List<String> getDatesSince(String fromDate) {
        List<String> dates = new ArrayList<String>();
        DateTime then = dateFormat.withZoneUTC().parseDateTime(fromDate);
        String today = dateFormat.withZoneUTC().print(System.currentTimeMillis());
        DateTime todaysTime = dateFormat.withZoneUTC().parseDateTime(today);
        if (then.isAfter(todaysTime))
            throw new IllegalArgumentException("fromDate is after today");
        while (!today.equals(fromDate)) {
            dates.add(fromDate);
            then = dateFormat.withZoneUTC().parseDateTime(fromDate);
            String date = dateFormat.withZoneUTC().print(then.plusDays(1));
            fromDate = date;
        }
        dates.add(today);
        return dates;
    }

    public static void main(final String[] args) {
        System.out.println(getDatesSince("20130615"));
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
        String lastDate = guestService.getApiKeyAttribute(updateInfo.apiKey, "lastDate");
        updateConnectorDataSince(updateInfo, lastDate);
    }


}
