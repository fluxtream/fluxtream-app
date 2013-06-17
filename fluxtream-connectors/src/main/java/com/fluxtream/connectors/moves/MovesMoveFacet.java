package com.fluxtream.connectors.moves;

import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractLocalTimeFacet;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 23:28
 */
@Entity(name="Facet_MovesMove")
@ObjectTypeSpec(name = "move", value = 1, extractor=MovesFacetExtractor.class, parallel=true, prettyname = "Moves")
public class MovesMoveFacet  extends AbstractLocalTimeFacet {

    @ElementCollection(fetch= FetchType.EAGER)
    @CollectionTable(
            name = "Activity",
            joinColumns = @JoinColumn(name="ActivityID")
    )
    public List<Activity> activities;

    public MovesMoveFacet() {}

    public MovesMoveFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
