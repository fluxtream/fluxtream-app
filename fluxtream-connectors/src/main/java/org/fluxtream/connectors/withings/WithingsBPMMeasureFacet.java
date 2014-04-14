package org.fluxtream.connectors.withings;

import javax.persistence.Entity;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_WithingsBPMMeasure")
@ObjectTypeSpec(name = "blood_pressure", value = 2, prettyname = "Blood Pressure Measures")
@Indexed
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
