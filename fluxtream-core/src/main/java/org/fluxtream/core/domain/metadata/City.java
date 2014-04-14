package org.fluxtream.core.domain.metadata;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;

@Entity(name="cities1000")
@NamedQueries ( {
    @NamedQuery( name="city.byNameStateAndCountryCode",
                 query="SELECT city from cities1000 city WHERE city.geo_name=? AND " +
                       "city.geo_admin1_code=? AND city.geo_country_code=?"),
	@NamedQuery( name="city.byNameAndCountryCode",
			query="SELECT city from cities1000 city WHERE city.geo_name=? AND city.geo_country_code=?")
})
public class City {
	
	@Id
	public Long geo_id;

	public String geo_timezone;
	
	@Index(name="name")
	public String geo_name;
	public double geo_latitude;
	public double geo_longitude;
	public String geo_country_code;
	public String geo_admin1_code;
	public long population;
	
	public String toString() {
		return geo_name;
	}
	
}
