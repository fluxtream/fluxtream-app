package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_BodymediaSteps")
@ObjectTypeSpec(name = "steps", value = 2, prettyname = "Steps")
@NamedQueries({
	@NamedQuery(name = "bodymedia.steps.deleteAll", query = "DELETE FROM Facet_BodymediaSteps facet WHERE facet.guestId=?"),
	@NamedQuery(name = "bodymedia.steps.between", query = "SELECT facet FROM Facet_BodymediaSteps facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class BodymediaStepsFacet extends AbstractFacet {
	
	@Override
	protected void makeFullTextIndexable() {}
	
}
