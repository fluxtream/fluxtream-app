package org.fluxtream.connectors.fluxtream_capture;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * Created by candide on 11/02/15.
 */
@Entity(name="Facet_FluxtreamCaptureObservation")
@ObjectTypeSpec(name = "observation", value = 2, parallel=false, prettyname = "Observation")
public class FluxtreamObservationFacet extends AbstractFacet {

    // NotNull
    public String fluxtreamId;

    // User-friendly name of Mymee "topic" -- the name of the Mymee button used to make the observation.
    // See getChannelName for the datastore/URL version of this (datastore puts each topic in a different channel)
    // NotNull

    public String timeZone;

    public Integer value;

    public String topicId;

    public long timeUpdatedOnDevice;

    public FluxtreamObservationFacet() {}

    public FluxtreamObservationFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
    }
}
