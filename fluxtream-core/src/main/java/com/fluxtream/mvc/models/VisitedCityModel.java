package com.fluxtream.mvc.models;

import java.util.Calendar;
import java.util.TimeZone;
import javax.xml.bind.DatatypeConverter;
import com.fluxtream.Configuration;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.VisitedCity;
import org.apache.commons.lang.WordUtils;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * User: candide
 * Date: 27/05/13
 * Time: 15:14
 */
public class VisitedCityModel {

    public String source;
    public String name;
    public String state;
    public String country;
    public String description;
    public String timezone;
    public long visitedCityId;
    int startMinute, endMinute;
    String startTime, endTime;
    long count;

    static final DateTimeFormatter fmt = DateTimeFormat.forPattern("MMM dd, HH:mm' 'a");

    public DurationModel duration;

    public VisitedCityModel(VisitedCity vcity,  Configuration env) {
        this.visitedCityId = vcity.getId();
        source = vcity.locationSource.toString();
        City city = vcity.city;
        name = city.geo_name;
        country = env.getCountry(city.geo_country_code);
        if (country == null)
            return;
        country = WordUtils.capitalize(country.toLowerCase());
        state = city.geo_admin1_code;
        if (state == null || state.equals(""))
            state = "-";

        description = city.geo_name;
        if ("US".equals(city.geo_country_code))
            description = city.geo_name + "/" + state + "/" + country;
        else
            description = city.geo_name + "/" + country;
        duration = new DurationModel((int)(vcity.end - vcity.start)/1000);

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone(vcity.city.geo_timezone));
        c.setTimeInMillis(vcity.start);
        this.startMinute = c.get(Calendar.HOUR_OF_DAY)*60 + c.get(Calendar.MINUTE);
        c.setTimeInMillis(vcity.end);
        this.endMinute = c.get(Calendar.HOUR_OF_DAY)*60 + c.get(Calendar.MINUTE);
        this.count = vcity.count;

        TimeZone tz = TimeZone.getTimeZone(vcity.city.geo_timezone);
        timezone = tz.getDisplayName(true, TimeZone.LONG);

        Calendar calendar = DatatypeConverter.parseDateTime(vcity.startTimeStorage);
        this.startTime = StringUtils.capitalise(fmt.print(calendar.getTimeInMillis()).toLowerCase());
        calendar = DatatypeConverter.parseDateTime(vcity.endTimeStorage);
        this.endTime = StringUtils.capitalise(fmt.print(calendar.getTimeInMillis()).toLowerCase());

    }

}
