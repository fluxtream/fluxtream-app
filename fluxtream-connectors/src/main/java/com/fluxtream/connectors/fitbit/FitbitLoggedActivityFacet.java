package com.fluxtream.connectors.fitbit;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_FitbitLoggedActivity")
@ObjectTypeSpec(name = "logged_activity", value = 2, extractor=FitbitFacetExtractor.class, prettyname = "Logged Activities")
@NamedQueries({
		@NamedQuery(name = "fitbit.logged_activity.deleteAll", query = "DELETE FROM Facet_FitbitLoggedActivity facet WHERE facet.guestId=?"),
		@NamedQuery(name = "fitbit.logged_activity.between", query = "SELECT facet FROM Facet_FitbitLoggedActivity facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class FitbitLoggedActivityFacet extends AbstractFacet {
	
	public long activityId;
	public long activityParentId;
	public int calories;
	public double distance;
	public int duration;
	public boolean isFavorite;
	public long logId;
	public String name;
	public int steps;
	public String date;
	
	@Override
	protected void makeFullTextIndexable() {}
		
}