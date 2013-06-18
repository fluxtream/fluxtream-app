package com.fluxtream.connectors.moves;

import javax.persistence.Entity;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 23:28
 */
@Entity(name="Facet_MovesMove")
@ObjectTypeSpec(name = "move", value = 1, extractor=MovesFacetExtractor.class, parallel=true, prettyname = "Moves")
public class MovesMoveFacet extends MovesFacet {

    public MovesMoveFacet() {}

    public MovesMoveFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
