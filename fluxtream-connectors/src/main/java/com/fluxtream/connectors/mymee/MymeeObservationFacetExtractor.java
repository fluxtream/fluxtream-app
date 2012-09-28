package com.fluxtream.connectors.mymee;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fluxtream.ApiData;
import com.fluxtream.Configuration;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.quantifiedmind.QuantifiedMindTestFacet;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class MymeeObservationFacetExtractor extends AbstractFacetExtractor {

    protected static DateTimeFormatter iso8601Formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeZone timeZone = DateTimeZone.forID("UTC");

    @Autowired
    Configuration env;

    @Override
    public List<AbstractFacet> extractFacets(final ApiData apiData, final ObjectType objectType) throws Exception {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

        JSONObject mymeeData = JSONObject.fromObject(apiData.json);
        JSONArray array = mymeeData.getJSONArray("rows");
        for(int i=0; i<array.size(); i++) {
            MymeeObservationFacet facet = new MymeeObservationFacet();

            JSONObject observationObject = array.getJSONObject(i);

            JSONObject valueObject = observationObject.getJSONObject("value");

            if (valueObject==null)
                continue;

            int timezoneOffset = valueObject.getInt("timezoneOffset");
            extractCommonFacetData(facet, apiData);

            final DateTime happened = iso8601Formatter.withZone(timeZone)
                    .parseDateTime(valueObject.getString("happened"));
            facet.start = happened.getMillis();
            facet.end = facet.start;

            facet.timezoneOffset = timezoneOffset;
            facet.mymeeId = observationObject.getString("id");
            facet.name = valueObject.getString("name");
            if (valueObject.has("note"))
                facet.note = valueObject.getString("note");
            if (valueObject.has("user"))
                facet.user = valueObject.getString("user");

            if (valueObject.has("unit"))
                facet.unit = valueObject.getString("unit");
            if (valueObject.has("baseunit"))
                facet.baseUnit = valueObject.getString("baseunit");

            if (valueObject.has("amount"))
                facet.amount = valueObject.getInt("amount");
            if (valueObject.has("baseAmount"))
                facet.baseAmount = valueObject.getInt("baseAmount");

            if (valueObject.has("_attachments")) {
                // we assume that there's only one attachment and that it's an image
                final JSONObject imageAttachment = valueObject.getJSONObject("_attachments");
                final String imageName = (String) imageAttachment.names().get(0);
                final String fetchURL = apiData.updateInfo.apiKey.getAttributeValue("fetchURL", env);
                final String baseURL = getBaseURL(fetchURL);
                final String mainDir = getMainDir(fetchURL);
                if (baseURL!=null&&mainDir!=null) {
                    facet.imageURL = new StringBuilder(baseURL).append("/")
                            .append(mainDir).append("/").append(facet.mymeeId)
                            .append("/").append(imageName).toString();
                }
            }

            facets.add(facet);
        }

        return facets;
    }

    public static String getBaseURL(String url) {
        try {
            URI uri = new URI(url);
            return (new StringBuilder(uri.getScheme()).append("://").append(uri.getHost()).toString());
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    public static String getMainDir(String url) {
        try {
            URI uri = new URI(url);
            final String[] splits = uri.getRawPath().split("/");
            if (splits.length>1)
                return splits[1];
        }
        catch (URISyntaxException e) {
            return null;
        }
        return null;
    }

    public static void main(final String[] args) {
        //String text = "https://bodytrack:groupW55@mymee.couchone.com/a_mymee/_design/mymee/_view/all-observations";
        //System.out.println(getBaseURL(text));
        //System.out.println(getMainDir(text));
        String datetime = "2012-09-27T16:45:59Z";
        final DateTime happened = iso8601Formatter.withZone(timeZone)
                .parseDateTime(datetime);
        System.out.println(happened.getMillis());
    }

}
