package org.fluxtream.connectors.fitbit;

import javax.persistence.Entity;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.domain.AbstractLocalTimeFacet;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_FitbitLoggedActivity")
@ObjectTypeSpec(name = "logged_activity", value = 2, extractor= FitbitActivityFacetExtractor.class, prettyname = "Logged Activities", isDateBased = true)
@Indexed
public class FitbitLoggedActivityFacet extends AbstractLocalTimeFacet {
	
	public long activityId;
	public long activityParentId;
	public int calories;
	public double distance;
	public int duration;
	public boolean isFavorite;
	public long logId;
	public String name;
	public int steps;

    public FitbitLoggedActivityFacet() {
        super();
    }

    public FitbitLoggedActivityFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {}

}
