package com.fluxtream.connectors.moves;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.domain.GuestSettings;
import org.apache.commons.lang.StringUtils;

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
    public float[] position = new float[2];

    @Override
    protected void fromFacet(final MovesPlaceFacet facet, final TimeInterval timeInterval, final GuestSettings settings)
            throws OutsideTimeBoundariesException {
        super.fromFacetBase(facet, timeInterval, settings);
        this.placeId = facet.placeId;
        this.type = facet.type;
        if (type==null||type.equals("unknown"))
            this.name = "Unknown Place";
        else if (type.equals("foursquare")||type.equals("user"))
            this.name = StringUtils.capitalize(facet.name);
        else if (type.equals("school")||type.equals("home")||type.equals("work"))
            this.name = StringUtils.capitalize(facet.type);
        this.foursquareId = facet.foursquareId;
        this.position[0] = facet.latitude;
        this.position[1] = facet.longitude;
    }

}
