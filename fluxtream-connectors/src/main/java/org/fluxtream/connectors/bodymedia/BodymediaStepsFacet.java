package org.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_BodymediaSteps")
@ObjectTypeSpec(name = "steps", value = 2, prettyname = "Steps", isDateBased = true)
@NamedQueries({
    @NamedQuery(name = "bodymedia.steps.getFailedUpdate", query = "SELECT facet FROM Facet_BodymediaSteps facet WHERE facet.guestId=? AND facet.lastSync=1"),
    @NamedQuery(name = "bodymedia.steps.getDaysPrior", query = "SELECT facet FROM Facet_BodymediaSteps facet WHERE facet.guestId=? AND facet.start<? ORDER BY facet.start DESC"),
    @NamedQuery(name = "bodymedia.steps.getByLastSync", query = "SELECT facet FROM Facet_BodymediaSteps facet WHERE facet.guestId=? ORDER BY facet.lastSync DESC")
})
@Indexed
public class BodymediaStepsFacet extends BodymediaAbstractFacet {

    //The total number of steps taken that day
    public int totalSteps;

    public BodymediaStepsFacet() {
        super();
    }

    public BodymediaStepsFacet(final long apiKeyId) {
        super(apiKeyId);
    }
}
