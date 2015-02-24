package org.fluxtream.connectors.misfit;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;

/**
 * Created by candide on 09/02/15.
 */
@Entity(name="Facet_MisfitActivitySession")
@ObjectTypeSpec(name = "activity_session", value = 2, prettyname = "Activity Session")
public class MisfitActivitySessionFacet extends AbstractFacet {

    public MisfitActivitySessionFacet() {}

    public MisfitActivitySessionFacet(long apiKeyId) {
        super(apiKeyId);
    }

    public String misfitId;

    @Index(name="activityType")
    public String activityType;

    public float points;
    public int steps;
    public float calories;
    public float distance;

    @Override
    protected void makeFullTextIndexable() {

    }

}
