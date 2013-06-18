package com.fluxtream.connectors.moves;

import javax.persistence.Entity;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 23:28
 */
@Entity(name="Facet_MovesPlace")
@ObjectTypeSpec(name = "place", value = 2, extractor=MovesFacetExtractor.class, parallel=true, prettyname = "Places")
public class MovesPlaceFacet extends MovesFacet  {

    public Long placeId;
    public String name;
    public String type;
    public String foursquareId;
    public float latitude, longitude;

    public MovesPlaceFacet() {}

    public MovesPlaceFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
