package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.Entity;

@Entity(name="Facet_FitbitLoggedActivity")
@ObjectTypeSpec(name = "logged_activity", value = 2, prettyname = "Logged Activities", isDateBased = true)
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
