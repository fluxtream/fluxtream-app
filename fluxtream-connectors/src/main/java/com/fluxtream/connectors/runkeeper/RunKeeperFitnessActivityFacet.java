package com.fluxtream.connectors.runkeeper;

import javax.persistence.Entity;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="Facet_RunKeeperFitnessActivity")
@ObjectTypeSpec(name = "fitnessActivity", value = 1, extractor=RunKeeperFitnessActivityExtractor.class, prettyname = "Fitness Activity", isDateBased = true)
public class RunKeeperFitnessActivityFacet extends AbstractFacet {

    public String uri;
    public String userID;
    public String type;
    public String equipment;
    public double total_distance;
    public int duration;
    public double[] distance;
    public double[] heart_rate;
    public double[] calories;
    public double total_climb;
    public String source;

    public double [] latitudes;
    public double [] longitudes;
    public long [] timestamps;
    public double [] altitudes;

    @Override
    protected void makeFullTextIndexable() {

    }

}
