package org.fluxtream.connectors.moves;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.connectors.location.LocationFacet;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 23:28
 */
@Entity(name="Facet_MovesPlace")
@ObjectTypeSpec(name = "place", value = 2, parallel=false, prettyname = "Places",
                locationFacetSource = LocationFacet.Source.MOVES, isDateBased = true)
public class MovesPlaceFacet extends MovesFacet  {

    public Long placeId;
    public String name;
    public String type;
    public String foursquareId;
    public float latitude, longitude;

    @ElementCollection(fetch= FetchType.EAGER)
    @CollectionTable(
            name = "MovesPlaceActivity",
            joinColumns = @JoinColumn(name="ActivityID")
    )
    public List<MovesActivity> activities;


    public MovesPlaceFacet() {
        this.api = Connector.getConnector("moves").value();
    }

    public MovesPlaceFacet(long apiKeyId) {
        super(apiKeyId);
        this.api = Connector.getConnector("moves").value();
    }

    @Override
    protected void makeFullTextIndexable() {
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
    List<MovesActivity> getActivities() {
        return activities;
    }
}
