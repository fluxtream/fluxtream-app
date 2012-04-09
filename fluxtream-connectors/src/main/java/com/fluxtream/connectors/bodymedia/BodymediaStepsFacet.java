package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_BodymediaSteps")
@ObjectTypeSpec(name = "steps", value = 2, prettyname = "Steps")
@NamedQueries({
})
@Indexed
public class BodymediaStepsFacet extends AbstractFacet {
	
	@Override
	protected void makeFullTextIndexable() {}
	
}
