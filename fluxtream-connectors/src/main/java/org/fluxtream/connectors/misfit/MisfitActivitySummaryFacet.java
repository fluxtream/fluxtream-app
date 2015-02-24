package org.fluxtream.connectors.misfit;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;

import javax.persistence.Entity;

/**
 * Created by candide on 09/02/15.
 */
@Entity(name="Facet_MisfitActivitySummary")
@ObjectTypeSpec(name = "activity_summary", value = 1, prettyname = "Activity Summary", isDateBased = true)
public class MisfitActivitySummaryFacet extends AbstractLocalTimeFacet {

    public MisfitActivitySummaryFacet() {}
    public MisfitActivitySummaryFacet(long apiKeyId) { super(apiKeyId); }

    public float points;
    public int steps;
    public float calories;
    public float activityCalories;
    public float distance;

    @Override
    protected void makeFullTextIndexable() {

    }

}
