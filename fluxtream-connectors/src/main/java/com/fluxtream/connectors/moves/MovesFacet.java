package com.fluxtream.connectors.moves;

import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import com.fluxtream.domain.AbstractLocalTimeFacet;

/**
 * User: candide
 * Date: 18/06/13
 * Time: 14:46
 */
@MappedSuperclass
public class MovesFacet extends AbstractLocalTimeFacet {
    @ElementCollection(fetch= FetchType.EAGER)
    @CollectionTable(
            name = "MovesActivity",
            joinColumns = @JoinColumn(name="ActivityID")
    )
    public List<MovesActivity> activities;

    public MovesFacet() {}

    public MovesFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
    }

}
