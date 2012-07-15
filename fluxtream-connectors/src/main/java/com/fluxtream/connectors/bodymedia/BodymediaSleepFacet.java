package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_BodymediaSleep")
@ObjectTypeSpec(name = "sleep", value = 4, prettyname = "sleep", extractor = BodymediaSleepFacetExtractor.class)
@NamedQueries({
	@NamedQuery(name = "bodymedia.sleep.deleteAll", query = "DELETE FROM Facet_BodymediaSleep facet WHERE facet.guestId=?"),
	@NamedQuery(name = "bodymedia.sleep.between", query = "SELECT facet FROM Facet_BodymediaSleep facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?"),
    @NamedQuery(name = "bodymedia.sleep.getFailedUpdate", query = "SELECT facet FROM Facet_BodymediaSleep facet WHERE facet.guestId=? AND facet.lastSync=1"),
    @NamedQuery(name = "bodymedia.sleep.getDaysPrior", query = "SELECT facet FROM Facet_BodymediaSleep facet WHERE facet.guestId=? AND facet.start<? ORDER BY facet.start DESC"),
    @NamedQuery(name = "bodymedia.sleep.getLastSync", query = "SELECT facet FROM Facet_BodymediaSleep facet WHERE facet.guestId=? ORDER BY facet.lastSync DESC")
})
@Indexed
public class BodymediaSleepFacet extends BodymediaAbstractFacet {

    //The sleep efficiency ratio provided by Bodymedia
    public double efficiency;
    //The total number of minutes spent lying awake
    public int totalLying;
    //The total number of minutes spent sleeping
    public int totalSleeping;

    @Override
	protected void makeFullTextIndexable() {}

}