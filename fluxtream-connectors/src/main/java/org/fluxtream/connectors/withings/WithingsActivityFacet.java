package org.fluxtream.connectors.withings;

import javax.persistence.Entity;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.domain.AbstractLocalTimeFacet;
import org.hibernate.search.annotations.Indexed;

/**
 * User: candide
 * Date: 21/11/13
 * Time: 15:05
 */
@Entity(name="Facet_WithingsActivity")
@ObjectTypeSpec(name = "activity", value = 8, prettyname = "Activity", isDateBased = true)
@Indexed
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
