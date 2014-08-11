package org.fluxtream.connectors.fitbit;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Updatable;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_FitbitActivity")
@ObjectTypeSpec(name = "activity_summary", value = 1, extractor= FitbitActivityFacetExtractor.class, prettyname = "Activity Summary", isDateBased = true)
@NamedQueries({
		@NamedQuery(name = "fitbit.activity_summary.byDate",
				query = "SELECT facet FROM Facet_FitbitActivity facet WHERE facet.apiKeyId=? AND facet.date=?")
})
@Indexed
public class FitbitTrackerActivityFacet extends AbstractLocalTimeFacet implements Updatable {
	
	public int activeScore;
	public int caloriesOut;
	public int activityCalories;
	public int fairlyActiveMinutes;
	public int lightlyActiveMinutes;
	public int sedentaryMinutes;
	public int steps;
	public int floors;
	public double elevation;
	public int veryActiveMinutes;

	public double trackerDistance;
	public double loggedActivitiesDistance;
	public double totalDistance;
	public double veryActiveDistance;
	public double moderatelyActiveDistance;
	public double lightlyActiveDistance;
	public double sedentaryActiveDistance;
	
	@Lob
	public String stepsJson;
	
	@Lob
	public String caloriesJson;

    public FitbitTrackerActivityFacet() {
        super();
    }

    public FitbitTrackerActivityFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {}

	@Override
	public void update(AbstractUpdater updater, ApiKey apiKey) {
        //try {
        //    ((FitBitTSUpdater) updater).updateCaloriesIntraday(this, apiKey);
        //} catch (RateLimitReachedException exc) {}
	}

}