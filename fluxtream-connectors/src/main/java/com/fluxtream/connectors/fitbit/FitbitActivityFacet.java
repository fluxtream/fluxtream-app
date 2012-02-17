package com.fluxtream.connectors.fitbit;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.domain.AbstractFloatingTimeZoneFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Updatable;

@Entity(name="Facet_FitbitActivity")
@ObjectTypeSpec(name = "activity_summary", value = 1, extractor=FitbitFacetExtractor.class, prettyname = "Activity Summary")
@NamedQueries({
		@NamedQuery(name = "fitbit.activity_summary.byStartEnd",
				query = "SELECT facet FROM Facet_FitbitActivity facet WHERE facet.guestId=? AND facet.start=? and facet.end=?"),
		@NamedQuery(name = "fitbit.activity_summary.all",
				query = "SELECT facet FROM Facet_FitbitActivity facet WHERE facet.guestId=? ORDER BY facet.start DESC"),
		@NamedQuery(name = "fitbit.activity_summary.last",
				query = "SELECT facet FROM Facet_FitbitActivity facet WHERE facet.guestId=? ORDER BY facet.start DESC LIMIT 1"),
		@NamedQuery(name = "fitbit.activity_summary.oldest",
				query = "SELECT facet FROM Facet_FitbitActivity facet WHERE facet.guestId=? ORDER BY facet.start ASC LIMIT 1"),
		@NamedQuery(name = "fitbit.activity_summary.deleteAll", query = "DELETE FROM Facet_FitbitActivity facet WHERE facet.guestId=?"),
		@NamedQuery(name = "fitbit.activity_summary.between", query = "SELECT facet FROM Facet_FitbitActivity facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class FitbitActivityFacet extends AbstractFloatingTimeZoneFacet implements Updatable {
	
	public int activeScore;
	public int caloriesOut;
	public int activityCalories;
	public int fairlyActiveMinutes;
	public int lightlyActiveMinutes;
	public int sedentaryMinutes;
	public int steps;
	public int floors;
	public int elevation;
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
	
	public String date;

	@Override
	protected void makeFullTextIndexable() {}

	@Override
	public void update(AbstractUpdater updater, ApiKey apiKey) {
		try {
			((FitBitTSUpdater) updater).updateCaloriesIntraday(this, apiKey);
		} catch (RateLimitReachedException exc) {}
	}
	
}