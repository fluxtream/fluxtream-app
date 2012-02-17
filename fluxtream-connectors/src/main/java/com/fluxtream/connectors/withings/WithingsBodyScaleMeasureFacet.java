package com.fluxtream.connectors.withings;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_WithingsBodyScaleMeasure")
@ObjectTypeSpec(name = "weight", value = 1, extractor=WithingsFacetExtractor.class, prettyname = "Weight Measures")
@NamedQueries({
		@NamedQuery(name = "withings.weight.deleteAll", query = "DELETE FROM Facet_WithingsBodyScaleMeasure facet WHERE facet.guestId=?"),
		@NamedQuery(name = "withings.weight.between", query = "SELECT facet FROM Facet_WithingsBodyScaleMeasure facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class WithingsBodyScaleMeasureFacet extends AbstractFacet {
	
	public long measureTime;
	
	public float weight;
	public float height;
	public float fatFreeMass;
	public float fatRatio;
	public float fatMassWeight;

	public transient float systolic;
	public transient float diastolic;
	public transient float heartPulse;
	
	@Override
	protected void makeFullTextIndexable() {}
	
}
