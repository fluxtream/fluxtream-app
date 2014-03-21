package org.fluxtream.connectors.moves;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.domain.GuestSettings;
import org.apache.commons.lang.StringUtils;

/**
 * User: candide
 * Date: 20/06/13
 * Time: 17:35
 */
public class MovesPlaceFacetVO extends AbstractMovesFacetVO<MovesPlaceFacet> {

    public Long placeId;
    public String name;
    public String placeType;
    public String foursquareId;
    public float[] position = new float[2];

    @Override
    protected void fromFacet(final MovesPlaceFacet facet, final TimeInterval timeInterval, final GuestSettings settings)
            throws OutsideTimeBoundariesException {
        super.fromFacetBase(facet, timeInterval, settings);
        this.placeId = facet.placeId;
        this.placeType = facet.type;
        if (placeType==null||placeType.equals("unknown"))
            this.name = "Unknown Place";
        else if (placeType.equals("foursquare")||placeType.equals("user"))
            this.name = StringUtils.capitalize(facet.name);
        else if (placeType.equals("school")||placeType.equals("home")||placeType.equals("work"))
            this.name = StringUtils.capitalize(placeType);
        this.foursquareId = facet.foursquareId;
        this.position[0] = facet.latitude;
        this.position[1] = facet.longitude;
    }

    @Override
    protected boolean isShareable(final MovesPlaceFacet facet) {
        return facet.foursquareId!=null;
    }

}
