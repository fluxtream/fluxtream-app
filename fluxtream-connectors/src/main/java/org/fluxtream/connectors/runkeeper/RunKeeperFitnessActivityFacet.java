package org.fluxtream.connectors.runkeeper;

import javax.persistence.Entity;
import javax.persistence.Lob;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.connectors.location.LocationFacet;
import org.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="Facet_RunKeeperFitnessActivity")
@ObjectTypeSpec(name = "fitnessActivity", value = 2,
                prettyname = "Fitness Activity", isDateBased = false, locationFacetSource = LocationFacet.Source.RUNKEEPER)
    public class RunKeeperFitnessActivityFacet extends AbstractFacet {

    public String uri;
    public String userID;
    public String type;
    public String equipment;
    public double total_distance;
    public int duration;

    @Lob
    public String distanceStorage;
    @Lob
    public String heartRateStorage;
    @Lob
    public String caloriesStorage;

    public Integer averageHeartRate;
    public Double totalCalories;

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
