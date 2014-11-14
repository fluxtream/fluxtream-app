package org.fluxtream.connectors.withings;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;

import javax.persistence.Entity;

/**
 * User: candide
 * Date: 21/11/13
 * Time: 15:05
 */
@Entity(name="Facet_WithingsActivity")
@ObjectTypeSpec(name = "activity", value = 8, prettyname = "Activity", isDateBased = true)
public class WithingsActivityFacet extends AbstractLocalTimeFacet {

    public String timezone;
    public int steps;
    public float distance;
    public float calories;
    public float elevation;

    public WithingsActivityFacet() {
        super();
    }

    public WithingsActivityFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {}
}
