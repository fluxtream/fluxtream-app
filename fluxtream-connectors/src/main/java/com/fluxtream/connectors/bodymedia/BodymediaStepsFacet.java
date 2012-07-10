package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_BodymediaSteps")
@ObjectTypeSpec(name = "steps", value = 2, prettyname = "Steps", extractor = BodymediaStepFacetExtractor.class)
@NamedQueries({
	@NamedQuery(name = "bodymedia.steps.deleteAll", query = "DELETE FROM Facet_BodymediaSteps facet WHERE facet.guestId=?"),
	@NamedQuery(name = "bodymedia.steps.between", query = "SELECT facet FROM Facet_BodymediaSteps facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class BodymediaStepsFacet extends AbstractFacet {

    //The date that this facet represents
    public String date;

    //The total number of steps taken that day
    public int totalSteps;

    //The hourly data for steps taken

    @Lob
    public String stepJson;

    public void setDate(final String date)
    {
        this.date = date;
    }

    public void setTotalSteps(final int totalSteps)
    {
        this.totalSteps = totalSteps;
    }

    public void setStepJson(final String stepJson)
    {
        this.stepJson = stepJson;
    }

	@Override
	protected void makeFullTextIndexable() {}
	
}
