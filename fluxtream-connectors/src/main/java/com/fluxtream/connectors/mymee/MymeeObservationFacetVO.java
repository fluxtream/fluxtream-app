package com.fluxtream.connectors.mymee;

import java.awt.Dimension;
import java.util.Date;
import java.util.List;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.GuestSettings;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class MymeeObservationFacetVO extends AbstractPhotoFacetVO<MymeeObservationFacet> {

    public String mymeeId;
    public String name;
    public String note = "";
    public String user = "";
    public int timezoneOffset;
    public int amount;
    public int baseAmount;
    public String unit;
    public String baseUnit;
    public String imageURL;
    public double longitude;
    public double latitude;

    @Override
    protected void fromFacet(final MymeeObservationFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.timeZone);
        this.start = facet.start;
        this.mymeeId = facet.mymeeId;
        this.name = facet.name;
        this.note = facet.note;
        this.user = facet.user;
        this.timezoneOffset = facet.timezoneOffset;
        this.amount = facet.amount;
        this.baseAmount = facet.baseAmount;
        this.unit = facet.unit;
        this.baseUnit = facet.baseUnit;
        this.imageURL = facet.imageURL;
        this.longitude = facet.longitude;
        this.latitude = facet.latitude;
    }

    double round(double v) {
        return (double) Math.round(v * 100) / 100;
    }

    @Override
    public String getPhotoUrl() {
        return this.imageURL;
    }

    @Override
    public String getThumbnail(final int index) {
        // TODO: is a thumbnail version available?
        return this.imageURL;
    }

    @Override
    public List<Dimension> getThumbnailSizes() {
        // TODO
        return null;
    }
}
