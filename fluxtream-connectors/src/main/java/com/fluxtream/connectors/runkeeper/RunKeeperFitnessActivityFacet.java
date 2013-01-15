package com.fluxtream.connectors.runkeeper;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="Facet_RunKeeperFitnessActivity")
@NamedQueries({
      @NamedQuery(name = "runkeeper.fitnessactivity.deleteAll", query = "DELETE FROM Facet_RunKeeperFitnessActivity facet WHERE facet.guestId=?"),
      @NamedQuery(name = "runkeeper.fitnessactivity.between", query = "SELECT facet FROM Facet_RunKeeperFitnessActivity facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@ObjectTypeSpec(name = "fitnessActivity", value = 1, extractor=RunKeeperFitnessActivityExtractor.class, prettyname = "Fitness Activity", isDateBased = false)
    public class RunKeeperFitnessActivityFacet extends AbstractFacet {

    public String uri;
    public String userID;
    public String type;
    public String equipment;
    public double total_distance;
    public int duration;

    @ElementCollection
    @CollectionTable(
            name = "FitnessActivityDistance",
            joinColumns = @JoinColumn(name="FitnessActivityID")
    )
    public List<DistanceMeasure> distance;
    @ElementCollection
    @CollectionTable(
            name = "FitnessActivityHeartRate",
            joinColumns = @JoinColumn(name="FitnessActivityID")
    )
    public List<HeartRateMeasure> heart_rate;

    public double total_climb;
    public String source;
    public boolean is_live;
    public String comments;

    @Override
    protected void makeFullTextIndexable() {

    }

}
