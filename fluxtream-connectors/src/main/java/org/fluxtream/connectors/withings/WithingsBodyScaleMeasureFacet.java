package org.fluxtream.connectors.withings;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;

@Entity(name="Facet_WithingsBodyScaleMeasure")
@ObjectTypeSpec(name = "weight", value = 1, prettyname = "Weight Measures")
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

    public WithingsBodyScaleMeasureFacet() {
        super();
    }

    public WithingsBodyScaleMeasureFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {}
	
}
