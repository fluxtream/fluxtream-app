package org.fluxtream.connectors.sleep_as_android;


import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;

@Entity(name="Facet_SleepAsAndroidSleep")
@ObjectTypeSpec(name = "sleep", value = 1, prettyname = "Sleep Logs", extractor=SleepFacetExtractor.class)
public class SleepFacet extends AbstractFacet {
    @Override
    protected void makeFullTextIndexable() {

    }
}
