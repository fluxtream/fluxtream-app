package com.fluxtream.connectors.fitbit;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_FitbitLoggedActivity")
@ObjectTypeSpec(name = "logged_activity", value = 2, extractor= FitbitActivityFacetExtractor.class, prettyname = "Logged Activities", isDateBased = true)
@NamedQueries({
        @NamedQuery(name = "fitbit.logged_activity.newest", query = "SELECT facet FROM Facet_FitbitLoggedActivity facet WHERE facet.guestId=? ORDER BY facet.end DESC LIMIT 1"),
})
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