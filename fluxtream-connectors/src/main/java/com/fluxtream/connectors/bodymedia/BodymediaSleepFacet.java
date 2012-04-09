package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_BodymediaSleep")
@ObjectTypeSpec(name = "Sleep", value = 4, prettyname = "Sleep")
@NamedQueries({
})
@Indexed
public class BodymediaSleepFacet extends AbstractFacet {
	
	@Override
	protected void makeFullTextIndexable() {}

}