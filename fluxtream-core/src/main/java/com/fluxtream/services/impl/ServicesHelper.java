package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import com.fluxtream.Configuration;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.utils.TimeUtils;
import org.apache.commons.lang.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <p>
 * <code>ServicesHelper</code> does something...
 * </p>
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class ServicesHelper {


    @Autowired
    Configuration env;

    private static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    public boolean setTimeZone(DayMetadataFacet info, String timeZone) {
        boolean timezoneWasSet = true;
        if (timeZone != null) {
            if (info.timeZone == null) {
                info.timeZone = timeZone;
            } else if (!info.timeZone.equals(timeZone)) {
                // timeZone strings can be different but stand for the exact same time offset
                info.otherTimeZone = timeZone;
                TimeZone otherTz = TimeZone.getTimeZone(info.otherTimeZone);
                TimeZone tz = TimeZone.getTimeZone(timeZone);
                int otherOffset = otherTz.getRawOffset();
                int offset = tz.getRawOffset();
                int otherDSTSavings = otherTz.getDSTSavings();
                int dSTSavings = tz.getDSTSavings();
                timezoneWasSet = !(otherOffset == offset && otherDSTSavings == dSTSavings);
                if (!timezoneWasSet)
                    info.otherTimeZone = null;
            }
            // TODO: we are using the "main" timezone but... shouldn't we be
            // more cautious?
            TimeZone tz = TimeZone.getTimeZone(info.timeZone);
            DateTime time = formatter.withZone(DateTimeZone.forTimeZone(tz))
                    .parseDateTime(info.date);
            info.start = TimeUtils.fromMidnight(time.getMillis(), tz);
            info.end = TimeUtils.toMidnight(time.getMillis(), tz);
        }
        return timezoneWasSet;
    }

    String addCity(DayMetadataFacet info, City city) {
        String country = env.getCountry(city.geo_country_code);
        if (country == null)
            return city.geo_name;
        country = WordUtils.capitalize(country.toLowerCase());
        String state = city.geo_admin1_code;
        if (state == null || state.equals(""))
            state = "-";
        String cityLabel = city.geo_name;
        if ("US".equals(city.geo_country_code))
            cityLabel = city.geo_name + "/" + state + "/" + country;
        else
            cityLabel = city.geo_name + "/" + country;

        if (info.cities == null || info.cities.length() == 0) {
            info.cities = cityLabel + "/" + city.population + "/1";
        } else if (info.cities.indexOf(cityLabel) == -1) {
            info.cities += "|" + cityLabel + "/" + city.population + "/1";
        } else {
            info.cities = incrementCityCount(info.cities, cityLabel);
        }
        return city.geo_name;
    }

    private String incrementCityCount(String cities, String cityLabel) {
        StringTokenizer st = new StringTokenizer(cities, "|");
        List<String> citiesList = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String city = st.nextToken();
            if (city.startsWith(cityLabel)) {
                String populationAndCount = city
                        .substring(cityLabel.length() + 1);
                String population = populationAndCount.split("/")[0];
                String countString = populationAndCount.split("/")[1];
                int count = Integer.valueOf(countString);
                citiesList
                        .add(cityLabel + "/" + population + "/" + (count + 1));
            } else {
                citiesList.add(city);
            }
        }
        StringBuffer sb = new StringBuffer();
        for (String city : citiesList) {
            if (sb.length() > 0)
                sb.append("|");
            sb.append(city);
        }
        return sb.toString();
    }
}
