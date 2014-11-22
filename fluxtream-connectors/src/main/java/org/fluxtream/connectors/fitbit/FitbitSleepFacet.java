package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity(name="Facet_FitbitSleep")
@ObjectTypeSpec(name = "sleep", value = 4, prettyname = "Sleep", isDateBased = true)
@NamedQueries({
		@NamedQuery(name = "fitbit.sleep.byDate",
				query = "SELECT facet FROM Facet_FitbitSleep facet WHERE facet.apiKeyId=? AND facet.date=?")
})
public class FitbitSleepFacet extends AbstractLocalTimeFacet {

	public boolean isMainSleep;

    @Index(name="logId")
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
