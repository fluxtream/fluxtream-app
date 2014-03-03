package org.fluxtream.domain.metadata;

import javax.persistence.Entity;
import org.fluxtream.domain.AbstractEntity;
import org.hibernate.annotations.Index;

/**
 * User: candide
 * Date: 24/06/13
 * Time: 14:03
 */
@Entity(name="FoursquareVenue")
public class FoursquareVenue extends AbstractEntity {

    @Index(name="foursquareId")
    public String foursquareId;
    public String name;
    public String canonicalUrl;
    public String categoryIconUrlPrefix;
    public String categoryIconUrlSuffix;
    public String categoryName;
    public String categoryFoursquareId;
    public String categoryShortName;

    public FoursquareVenue() {}

}
