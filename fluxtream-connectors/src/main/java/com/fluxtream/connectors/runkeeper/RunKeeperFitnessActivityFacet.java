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
    @Override
    protected void makeFullTextIndexable() {
    }
}
