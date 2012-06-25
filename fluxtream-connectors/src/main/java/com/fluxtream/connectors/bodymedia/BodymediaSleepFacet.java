package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_BodymediaSleep")
@ObjectTypeSpec(name = "sleep", value = 4, prettyname = "sleep", extractor = BodymediaFacetExtractor.class)
@NamedQueries({
	@NamedQuery(name = "bodymedia.sleep.deleteAll", query = "DELETE FROM Facet_BodymediaSleep facet WHERE facet.guestId=?"),
	@NamedQuery(name = "bodymedia.sleep.between", query = "SELECT facet FROM Facet_BodymediaSleep facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class BodymediaSleepFacet extends AbstractFacet {

    //The date that this facet represents
    String date;
    //The sleep efficiency ratio provided by Bodymedia
    double efficiency;
    //The total number of minutes spent lying awake
    int totalLying;
    //The total number of minutes spent sleeping
    int totalSleeping;
    //The Json for the sleep periods;
    String sleepJson;

    public void setDate(final String date)
    {
        this.date = date;
    }

    public void setEfficiency(final double efficiency)
    {
        this.efficiency = efficiency;
    }

    public void setTotalLying(final int totalLying)
    {
        this.totalLying = totalLying;
    }

    public void setTotalSleeping(final int totalSleeping)
    {
        this.totalSleeping = totalSleeping;
    }

    public void setSleepJson(final String sleepJson)
    {
        this.sleepJson = sleepJson;
    }

    @Override
	protected void makeFullTextIndexable() {}

}