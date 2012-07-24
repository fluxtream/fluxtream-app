package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.hibernate.search.annotations.Indexed;

/**
 * Stores data from the bodymedia burn api
 */
@Entity(name="Facet_BodymediaBurn")
@ObjectTypeSpec(name = "burn", value = 1, prettyname = "Calories Burned", extractor = BodymediaBurnFacetExtractor.class)
@NamedQueries({
	@NamedQuery(name = "bodymedia.burn.deleteAll", query = "DELETE FROM Facet_BodymediaBurn facet WHERE facet.guestId=?"),
	@NamedQuery(name = "bodymedia.burn.between", query = "SELECT facet FROM Facet_BodymediaBurn facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?"),
    @NamedQuery(name = "bodymedia.burn.getFailedUpdate", query = "SELECT facet FROM Facet_BodymediaBurn facet WHERE facet.guestId=? AND facet.lastSync=1"),
    @NamedQuery(name = "bodymedia.burn.getDaysPrior", query = "SELECT facet FROM Facet_BodymediaBurn facet WHERE facet.guestId=? AND facet.start<? ORDER BY facet.start DESC"),
    @NamedQuery(name = "bodymedia.burn.getByLastSync", query = "SELECT facet FROM Facet_BodymediaBurn facet WHERE facet.guestId=? ORDER BY facet.lastSync DESC")
})
@Indexed
public class BodymediaBurnFacet extends BodymediaAbstractFacet {

    public int totalCalories = 0;
    public int estimatedCalories = 0;
    public int predictedCalories = 0;

    public void setTotalCalories(final int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public void setEstimatedCalories(final int estimatedCalories) {
        this.estimatedCalories = estimatedCalories;
    }

    public void setPredictedCalories(final int predictedCalories) {
        this.predictedCalories = predictedCalories;
    }
}