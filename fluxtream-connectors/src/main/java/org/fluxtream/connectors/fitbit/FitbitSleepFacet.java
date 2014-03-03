package org.fluxtream.connectors.fitbit;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.domain.AbstractLocalTimeFacet;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_FitbitSleep")
@ObjectTypeSpec(name = "sleep", value = 4, extractor=FitbitSleepFacetExtractor.class, prettyname = "Sleep", isDateBased = true)
@NamedQueries({
		@NamedQuery(name = "fitbit.sleep.byDate",
				query = "SELECT facet FROM Facet_FitbitSleep facet WHERE facet.apiKeyId=? AND facet.date=?")
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
