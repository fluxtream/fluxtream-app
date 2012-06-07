package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;
import net.sf.json.JSONArray;
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
    // Store the JSON for the minutely data.  This is a JSON array with one entry per minute
    /*
     * Not really. stores a JSONArray of days that in turn store JSON arrays of minutely data
     */
    @Lob
    public JSONArray days;

    public BodymediaBurnFacet(){
        averageCalories = 0;
        totalCalories = 0;
        //this.api must be set to 88 for the app to properly find it in the hashtable. I don't know why
        this.api = 88;
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

    public JSONArray getDays() {
         return days;
     }

     public void setDays(final JSONArray minutes) {
         this.days = minutes;
     }

}