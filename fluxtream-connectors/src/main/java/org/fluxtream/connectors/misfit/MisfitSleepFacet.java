package org.fluxtream.connectors.misfit;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.Lob;

/**
 * Created by candide on 09/02/15.
 */
@Entity(name="Facet_MisfitSleep")
@ObjectTypeSpec(name = "sleep", value = 4, prettyname = "Sleep", isDateBased = true)
public class MisfitSleepFacet extends AbstractFacet {

    public String misfitId;
    public boolean autodetected;

    @Index(name="date_idx")
    public String date;

    @Lob
    public String sleepDetails;

    public MisfitSleepFacet() {}

    public MisfitSleepFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {

    }

}
