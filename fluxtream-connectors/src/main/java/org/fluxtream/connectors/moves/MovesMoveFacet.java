package org.fluxtream.connectors.moves;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.connectors.location.LocationFacet;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 23:28
 */
@Entity(name="Facet_MovesMove")
@ObjectTypeSpec(name = "move", value = 1, parallel=false, prettyname = "Moves",
        locationFacetSource = LocationFacet.Source.MOVES, isDateBased = true)
public class MovesMoveFacet extends MovesFacet {

    @ElementCollection(fetch= FetchType.EAGER)
    @CollectionTable(
            name = "MovesMoveActivity",
            joinColumns = @JoinColumn(name="ActivityID")
    )
    public List<MovesActivity> activities;

    public MovesMoveFacet() {}

    public MovesMoveFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    void addActivity(final MovesActivity activity) {
        if (activities==null)
            activities = new ArrayList<MovesActivity>();
        activities.add(activity);
    }

    @Override
    void removeActivity(final MovesActivity activity) {
        activities.remove(activity);
    }

    @Override
    protected void makeFullTextIndexable() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    List<MovesActivity> getActivities() {
        return activities;
    }

}
