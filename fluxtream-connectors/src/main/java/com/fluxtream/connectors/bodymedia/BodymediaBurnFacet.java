package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_BodymediaBurn")
@ObjectTypeSpec(name = "burn", value = 1, prettyname = "Calories Burned", extractor = BodymediaFacetExtractor.class)
@NamedQueries({
	@NamedQuery(name = "bodymedia.burn.deleteAll", query = "DELETE FROM Facet_BodymediaBurn facet WHERE facet.guestId=?"),
	@NamedQuery(name = "bodymedia.burn.between", query = "SELECT facet FROM Facet_BodymediaBurn facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class BodymediaBurnFacet extends AbstractFacet {

    public int averageCalories;
    public int totalCalories;
    public int estimatedCalories;
    public int predictedCalories;
    // Store the JSON for the minutely data.  This is a JSON array with one entry per minute
    @Lob
    public String minutes;

	
	@Override
	protected void makeFullTextIndexable() {}
	
}