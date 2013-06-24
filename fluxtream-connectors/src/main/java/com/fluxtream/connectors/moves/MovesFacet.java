package com.fluxtream.connectors.moves;

import java.util.List;
import javax.persistence.MappedSuperclass;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.AbstractLocalTimeFacet;

/**
 * User: candide
 * Date: 18/06/13
 * Time: 14:46
 */
@MappedSuperclass
public abstract class MovesFacet extends AbstractLocalTimeFacet {

    public MovesFacet() {
        this.api = Connector.getConnector("moves").value();
    }

    public MovesFacet(long apiKeyId) {
        super(apiKeyId);
        this.api = Connector.getConnector("moves").value();
    }

    abstract void addActivity(MovesActivity activity);

    abstract void removeActivity(final MovesActivity movesActivity);

    abstract List<MovesActivity> getActivities();

    @Override
    protected void makeFullTextIndexable() {
    }
}
