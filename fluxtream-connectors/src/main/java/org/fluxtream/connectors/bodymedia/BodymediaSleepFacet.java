package org.fluxtream.connectors.bodymedia;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity(name="Facet_BodymediaSleep")
@ObjectTypeSpec(name = "sleep", value = 4, prettyname = "sleep", isDateBased = true)
@NamedQueries({
    @NamedQuery(name = "bodymedia.sleep.getFailedUpdate", query = "SELECT facet FROM Facet_BodymediaSleep facet WHERE facet.guestId=? AND facet.lastSync=1"),
    @NamedQuery(name = "bodymedia.sleep.getDaysPrior", query = "SELECT facet FROM Facet_BodymediaSleep facet WHERE facet.guestId=? AND facet.start<? ORDER BY facet.start DESC"),
    @NamedQuery(name = "bodymedia.sleep.getByLastSync", query = "SELECT facet FROM Facet_BodymediaSleep facet WHERE facet.guestId=? ORDER BY facet.lastSync DESC")
})
public class BodymediaSleepFacet extends BodymediaAbstractFacet {

    //The sleep efficiency ratio provided by Bodymedia
    public double efficiency;
    //The total number of minutes spent lying awake
    public int totalLying;
    //The total number of minutes spent sleeping
    public int totalSleeping;

    public BodymediaSleepFacet() {
        super();
    }

    public BodymediaSleepFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {}

}