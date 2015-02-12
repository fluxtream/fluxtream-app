package org.fluxtream.connectors.fluxtream_capture;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * Created by candide on 11/02/15.
 */
@Entity(name="Facet_FluxtreamObservation")
@ObjectTypeSpec(name = "observation", value = 1, isImageType=true, parallel=false, prettyname = "Observation")
@NamedQueries({
        @NamedQuery(name = "flx.observation.byMymeeId", query = "SELECT facet FROM Facet_FluxtreamObservation facet WHERE facet.guestId=? AND facet.fluxtreamId=?"),
        @NamedQuery(name = "flx.photo.between", query = "SELECT facet FROM Facet_FluxtreamObservation facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=? AND facet.imageURL IS NOT NULL")
})
public class FluxtreamObservationFacet extends AbstractFacet {

    // NotNull
    public String fluxtreamId;

    // User-friendly name of Mymee "topic" -- the name of the Mymee button used to make the observation.
    // See getChannelName for the datastore/URL version of this (datastore puts each topic in a different channel)
    // NotNull
    public String name;

    public String note;
    public String user;

    public Integer timezoneOffset;

    public Double amount;
    public Integer baseAmount;
    public String unit;
    public String baseUnit;

    public String imageURL;

    public Double latitude;
    public Double longitude;

    public FluxtreamObservationFacet() {}

    public FluxtreamObservationFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
    }

    // Returns the channel name used by datastore and in datastore-related URLs
    public String getChannelName() {
        // Datastore channel names have all characters that aren't alphanumeric or underscores replaced with underscores
        return name.replaceAll("[^0-9a-zA-Z_]+", "_");
    }
}
