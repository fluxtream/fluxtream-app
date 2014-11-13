package org.fluxtream.connectors.withings;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;

@Entity(name="Facet_WithingsBPMMeasure")
@ObjectTypeSpec(name = "blood_pressure", value = 2, prettyname = "Blood Pressure Measures")
public class WithingsBPMMeasureFacet extends AbstractFacet {
	
	public long measureTime;
	
	public float systolic;
	public float diastolic;
	public float heartPulse;

    public WithingsBPMMeasureFacet() {
        super();
    }

    public WithingsBPMMeasureFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {}
	
}
