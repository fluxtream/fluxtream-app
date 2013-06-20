package com.fluxtream.connectors.moves;

import javax.persistence.Entity;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.connectors.location.LocationFacet;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 23:28
 */
@Entity(name="Facet_MovesPlace")
@ObjectTypeSpec(name = "place", value = 2, extractor=MovesFacetExtractor.class, parallel=false, prettyname = "Places",
                locationFacetSource = LocationFacet.Source.MOVES)
public class MovesPlaceFacet extends MovesFacet  {

    public Long placeId;
    public String name;
    public String type;
    public String foursquareId;
    public float latitude, longitude;

    public MovesPlaceFacet() {
        this.api = Connector.getConnector("moves").value();
    }

    public MovesPlaceFacet(long apiKeyId) {
        super(apiKeyId);
        this.api = Connector.getConnector("moves").value();
    }

    @Override
    protected void makeFullTextIndexable() {
        if (name!=null)
            fullTextDescription = name;
    }
}
