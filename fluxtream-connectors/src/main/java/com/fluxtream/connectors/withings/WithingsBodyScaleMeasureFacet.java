package com.fluxtream.connectors.withings;

import javax.persistence.Entity;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_WithingsBodyScaleMeasure")
@ObjectTypeSpec(name = "weight", value = 1, extractor=WithingsFacetExtractor.class, prettyname = "Weight Measures")
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
