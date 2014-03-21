package org.fluxtream.connectors.up;

import java.util.Locale;
import org.fluxtream.connectors.updaters.UpdateFailedException;
import org.fluxtream.utils.UnexpectedHttpResponseCodeException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

/**
 * User: candide
 * Date: 03/02/14
 * Time: 11:26
 */
public class JawboneUpUpdaterTest {


    private long getBeginningOfTime() {
        return ISODateTimeFormat.basicDate().withZoneUTC().parseMillis("20100101");
    }

    @Test
    public void simpleTest() {
        final String s = "2014020413";
        String dateString = s.substring(0, 8);
        String hour_of_day = s.substring(8);
        System.out.println("date: " + dateString);
        System.out.println("hour: " + hour_of_day);
    }

    //@Test
    public void test() {
        final HttpClient client = new DefaultHttpClient();
        try {
            long now = System.currentTimeMillis()/1000;
            System.out.println(now);
            long beginningOfTime = getBeginningOfTime()/1000;
            long friday = ISODateTimeFormat.basicDate().withZoneUTC().parseMillis("20140131")/1000;
            long saturday = ISODateTimeFormat.basicDate().withZoneUTC().parseMillis("20140201")/1000;
            long sunday = ISODateTimeFormat.basicDate().withZoneUTC().parseMillis("20140202")/1000;
            long monday = ISODateTimeFormat.basicDate().withZoneUTC().parseMillis("20140203")/1000;
            HttpGet get = new HttpGet(String.format("https://jawbone.com/nudge/api/v.1.0/users/@me/moves?start_time=%s&updated_after=%s", beginningOfTime, "1391432752"));
            get.setHeader("Authorization", "Bearer b6_3pfGGwEhBVmnMx7TBfdpj72FdmizX1SAGFutknc_gduJsmw0YrYmjV9_oVcGdLeXXdbUpUeld6BVfbotfEVECdgRlo_GULMgGZS0EumxrKbZFiOmnmAPChBPDZ5JP");
            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String content = responseHandler.handleResponse(response);
                JSONObject json = JSONObject.fromObject(content);
                JSONObject data = json.getJSONObject("data");
                JSONArray items = data.getJSONArray("items");
                for (int i=0; i<items.size(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    System.out.println(item.getString("date"));
                    JSONObject details = item.getJSONObject("details");
                    System.out.println(details.getString("steps"));
                    System.out.println(details.getString("wo_count"));
                    long timeCreated = item.getLong("time_created")*1000;
                    long timeUpdated = item.getLong("time_updated")*1000;
                    long timeCompleted = item.getLong("time_completed")*1000;
                    System.out.println(DateTimeFormat.forStyle("LL").withLocale(Locale.FRANCE).print(timeCreated));
                    System.out.println(DateTimeFormat.forStyle("LL").withLocale(Locale.FRANCE).print(timeUpdated));
                    System.out.println(DateTimeFormat.forStyle("LL").withLocale(Locale.FRANCE).print(timeCompleted));
                    System.out.println("----");
                    System.out.println();
                }
            } else {
                handleErrors(statusCode, response, "ah szut alors");
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    private void handleErrors(final int statusCode, final HttpResponse response, final String message) throws Exception {
        // try to extract more information from the response
        try {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String content = responseHandler.handleResponse(response);
            JSONObject errorJson = JSONObject.fromObject(content);
            if (errorJson.has("meta")) {
                JSONObject meta = errorJson.getJSONObject("meta");
                if (meta.has("error_type")) {
                    String details = meta.has("error_detail") ? meta.getString("error_details") : "Unknown Error (no details provided)";
                    throw new UpdateFailedException(message + " - " + details, true);
                }
            }
        } catch (Throwable t) {
            // just ignore any potential problems here
        }
        throw new UnexpectedHttpResponseCodeException(statusCode, message + " - unexpected status code: " + statusCode);
    }

}
