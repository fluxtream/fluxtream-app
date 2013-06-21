package com.fluxtream.connectors.moves;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.domain.GuestSettings;

/**
 * User: candide
 * Date: 20/06/13
 * Time: 17:35
 */
public class MovesPlaceFacetVO extends AbstractMovesFacetVO<MovesPlaceFacet> {

    public Long placeId;
    public String name;
    public String type;
    public String foursquareId;
    public float latitude, longitude;

    @Override
    protected void fromFacet(final MovesPlaceFacet facet, final TimeInterval timeInterval, final GuestSettings settings)
            throws OutsideTimeBoundariesException {
        super.fromFacetBase(facet, timeInterval, settings);
        this.placeId = facet.placeId;
        this.name = facet.name;
        this.type = facet.type;
        this.foursquareId = facet.foursquareId;
        this.latitude = facet.latitude;
        this.longitude = facet.longitude;
    }

}
