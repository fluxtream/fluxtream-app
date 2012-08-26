package com.fluxtream.connectors.fitbit;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFloatingTimeZoneFacet;
import org.hibernate.search.annotations.Indexed;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="Facet_FitbitBodyMeasurement")
@ObjectTypeSpec(name = "body", value = 8, extractor=FitbitFacetExtractor.class, prettyname = "Body Measurements")
@NamedQueries({
      @NamedQuery(name = "fitbit.body.byDate",
                  query = "SELECT facet FROM Facet_FitbitBodyMeasurement facet WHERE facet.guestId=? AND facet.date=?"),
      @NamedQuery(name = "fitbit.body.byStartEnd",
                  query = "SELECT facet FROM Facet_FitbitBodyMeasurement facet WHERE facet.guestId=? AND facet.start=? AND facet.end=?"),
      @NamedQuery(name = "fitbit.body.newest",
                  query = "SELECT facet FROM Facet_FitbitBodyMeasurement facet WHERE facet.guestId=? and facet.isEmpty=false ORDER BY facet.end DESC LIMIT 1"),
      @NamedQuery(name = "fitbit.body.oldest",
                  query = "SELECT facet FROM Facet_FitbitBodyMeasurement facet WHERE facet.guestId=? and facet.isEmpty=false ORDER BY facet.start ASC LIMIT 1"),
      @NamedQuery(name = "fitbit.body.deleteAll", query = "DELETE FROM Facet_FitbitBodyMeasurement facet WHERE facet.guestId=?"),
      @NamedQuery(name = "fitbit.body.between", query = "SELECT facet FROM Facet_FitbitBodyMeasurement facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=? and facet.isEmpty=false")
})

@Indexed
public class FitbitBodyMeasurementFacet extends AbstractFloatingTimeZoneFacet {

    public double bicep;
    public double bmi;
    public double calf;
    public double chest;
    public double fat;
    public double forearm;
    public double hips;
    public double neck;
    public double thigh;
    public double waist;
    public double weight;

    public String date;

    @Override
    protected void makeFullTextIndexable() {}
}
