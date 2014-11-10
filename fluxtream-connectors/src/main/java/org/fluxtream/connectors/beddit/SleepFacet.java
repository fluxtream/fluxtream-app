package org.fluxtream.connectors.beddit;


import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;

@Entity(name="Facet_BedditSleep")
@ObjectTypeSpec(name = "sleep", value = 1, prettyname = "Sleep Logs")
public class SleepFacet extends AbstractFacet {
    @Override
    protected void makeFullTextIndexable() {

    }
}