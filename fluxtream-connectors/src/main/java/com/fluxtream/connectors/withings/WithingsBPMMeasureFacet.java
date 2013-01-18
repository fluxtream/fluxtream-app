package com.fluxtream.connectors.withings;

import javax.persistence.Entity;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_WithingsBPMMeasure")
@ObjectTypeSpec(name = "blood_pressure", value = 2, extractor=WithingsFacetExtractor.class, prettyname = "Blood Pressure Measures")
@Indexed
public class WithingsBPMMeasureFacet extends AbstractFacet {
	
	public long measureTime;
	
	public float systolic;
	public float diastolic;
	public float heartPulse;
	
	@Override
	protected void makeFullTextIndexable() {}
	
}
