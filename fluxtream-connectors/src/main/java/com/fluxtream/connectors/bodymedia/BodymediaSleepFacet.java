package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_BodymediaSleep")
@ObjectTypeSpec(name = "Sleep", value = 4, prettyname = "Sleep")
@NamedQueries({
	@NamedQuery(name = "bodymedia.sleep.deleteAll", query = "DELETE FROM Facet_BodymediaSleep facet WHERE facet.guestId=?"),
	@NamedQuery(name = "bodymedia.sleep.between", query = "SELECT facet FROM Facet_BodymediaSleep facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class BodymediaSleepFacet extends AbstractFacet {
	
	@Override
	protected void makeFullTextIndexable() {}

}