package com.fluxtream.connectors.fitbit;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_FitbitSleep")
@ObjectTypeSpec(name = "sleep", value = 4, extractor=FitbitSleepFacetExtractor.class, prettyname = "Sleep", isDateBased = true)
@NamedQueries({
		@NamedQuery(name = "fitbit.sleep.byDate",
				query = "SELECT facet FROM Facet_FitbitSleep facet WHERE facet.guestId=? AND facet.date=?"),
		@NamedQuery(name = "fitbit.sleep.byStartEnd",
				query = "SELECT facet FROM Facet_FitbitSleep facet WHERE facet.guestId=? AND facet.start=? AND facet.end=?"),
		@NamedQuery(name = "fitbit.sleep.newest",
				query = "SELECT facet FROM Facet_FitbitSleep facet WHERE facet.guestId=? and facet.isEmpty=false ORDER BY facet.end DESC LIMIT 1"),
		@NamedQuery(name = "fitbit.sleep.oldest",
				query = "SELECT facet FROM Facet_FitbitSleep facet WHERE facet.guestId=? and facet.isEmpty=false ORDER BY facet.start ASC LIMIT 1"),
})

//SELECT * FROM Facet_FitbitSleep facet WHERE facet.guestId=1 ORDER BY facet.start ASC LIMIT 1
@Indexed
public class FitbitSleepFacet extends AbstractLocalTimeFacet {

	public boolean isMainSleep;
	public long logId;
	public int minutesToFallAsleep;
	public int minutesAfterWakeup;
	public int minutesAsleep;
	public int minutesAwake;
	public int awakeningsCount;
	public int timeInBed;
    public int duration;

    public FitbitSleepFacet() {
        super();
    }

    public FitbitSleepFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {}

}