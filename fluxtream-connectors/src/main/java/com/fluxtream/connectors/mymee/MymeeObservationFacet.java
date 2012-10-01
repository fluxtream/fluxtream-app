package com.fluxtream.connectors.mymee;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.connectors.quantifiedmind.QuantifiedMindTestFacetExtractor;
import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="Facet_MymeeObservation")
@ObjectTypeSpec(name = "observation", value = 1, extractor=MymeeObservationFacetExtractor.class, parallel=false, prettyname = "Observation")
@NamedQueries({
      @NamedQuery(name = "mymee.observation.byMymeeId", query = "SELECT facet FROM Facet_MymeeObservation facet WHERE facet.guestId=? AND facet.mymeeId=?"),
      @NamedQuery(name = "mymee.observation.deleteAll", query = "DELETE FROM Facet_MymeeObservation facet WHERE facet.guestId=?"),
      @NamedQuery(name = "mymee.observation.between", query = "SELECT facet FROM Facet_MymeeObservation facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
public class MymeeObservationFacet extends AbstractFacet {

    public String mymeeId;
    public String name;
    public String note;
    public String user;
    public int timezoneOffset;
    public int amount;
    public int baseAmount;
    public String unit;
    public String baseUnit;
    public String imageURL;

    public double latitude;
    public double longitude;

    @Override
    protected void makeFullTextIndexable() {
    }
}
