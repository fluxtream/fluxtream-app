package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_BodymediaBurn")
@ObjectTypeSpec(name = "burn", value = 1, prettyname = "Calories Burned")
@NamedQueries({
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