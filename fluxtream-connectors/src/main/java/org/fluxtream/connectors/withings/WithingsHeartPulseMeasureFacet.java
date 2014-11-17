package org.fluxtream.connectors.withings;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;

/**
 * User: candide
 * Date: 17/05/13
 * Time: 12:39
 */
@Entity(name="Facet_WithingsHeartPulseMeasure")
@ObjectTypeSpec(name = "heart_pulse", value = 4, prettyname = "Smart Body Analyzer Heart Rate Measure")
public class WithingsHeartPulseMeasureFacet extends AbstractFacet {

    public float heartPulse;

    public WithingsHeartPulseMeasureFacet() {}

    public WithingsHeartPulseMeasureFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {

    }
}
