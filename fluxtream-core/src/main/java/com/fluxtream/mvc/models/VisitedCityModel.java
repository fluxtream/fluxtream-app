package com.fluxtream.mvc.models;

import com.fluxtream.Configuration;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.VisitedCity;
import org.apache.commons.lang.WordUtils;

/**
 * User: candide
 * Date: 27/05/13
 * Time: 15:14
 */
public class VisitedCityModel {

    public String name;
    public String state;
    public String country;
    public String description;

    public DurationModel duration;

    public VisitedCityModel(VisitedCity vcity, Configuration env) {
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
    }

}
