package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;
import org.hibernate.search.annotations.Indexed;

/**
 * Stores data from the bodymedia burn api
 */
@Entity(name="Facet_BodymediaBurn")
@ObjectTypeSpec(name = "burn", value = 1, prettyname = "Calories Burned", extractor = BodymediaFacetExtractor.class)
@NamedQueries({
	@NamedQuery(name = "bodymedia.burn.deleteAll", query = "DELETE FROM Facet_BodymediaBurn facet WHERE facet.guestId=?"),
	@NamedQuery(name = "bodymedia.burn.between", query = "SELECT facet FROM Facet_BodymediaBurn facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class BodymediaBurnFacet extends AbstractFacet {

    public int averageCalories = 0;
    public int totalCalories = 0;
    public int estimatedCalories = 0;
    public int predictedCalories = 0;

    public String date;
    // Store the JSON for the minutely data.  This is a JSON array with one entry per minute
    /*
     * Not really. stores a JSONArray of burnJson that in turn store JSON arrays of minutely data
     */
    @Lob
    public String burnJson;

    public BodymediaBurnFacet(){
        averageCalories = 0;
        totalCalories = 0;
    }
	
	@Override
	protected void makeFullTextIndexable() {}

    public int getAverageCalories() {
        return averageCalories;
    }

    public void setAverageCalories(final int averageCalories) {
        this.averageCalories = averageCalories;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(final int totalCalories) {
        this.totalCalories = totalCalories;
    }

   public String getBurnJson() {
        return burnJson;
    }

    public void setBurnJson(final String minutes) {
        this.burnJson = minutes;
    }

    public void setEstimatedCalories(final int estimatedCalories) {
        this.estimatedCalories = estimatedCalories;
    }

    public void setPredictedCalories(final int predictedCalories) {
        this.predictedCalories = predictedCalories;
    }

    public void setDate(final String date) {
        this.date = date;
    }

}