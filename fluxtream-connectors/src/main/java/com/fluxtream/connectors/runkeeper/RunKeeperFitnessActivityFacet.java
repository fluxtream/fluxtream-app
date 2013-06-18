package com.fluxtream.connectors.runkeeper;

import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="Facet_RunKeeperFitnessActivity")
@ObjectTypeSpec(name = "fitnessActivity", value = 1, extractor=RunKeeperFitnessActivityExtractor.class,
                prettyname = "Fitness MovesActivity", isDateBased = false, locationFacetSource = LocationFacet.Source.RUNKEEPER)
    public class RunKeeperFitnessActivityFacet extends AbstractFacet {

    public String uri;
    public String userID;
    public String type;
    public String equipment;
    public double total_distance;
    public int duration;

    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(
            name = "FitnessActivityHeartRate",
            joinColumns = @JoinColumn(name="FitnessActivityID")
    )
    public List<HeartRateMeasure> heart_rate;

    public double total_climb;
    public String source;
    public boolean is_live;
    public String comments;
    public String timeZone;

    public RunKeeperFitnessActivityFacet() {
        super();
    }

    public RunKeeperFitnessActivityFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {

    }

}
