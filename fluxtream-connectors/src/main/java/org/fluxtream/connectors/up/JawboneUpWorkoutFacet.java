package org.fluxtream.connectors.up;

import javax.persistence.Entity;
import javax.persistence.Lob;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 07/02/14
 * Time: 11:34
 */
@Entity(name="Facet_JawboneUpWorkout")
@ObjectTypeSpec(name = "workout", value = 8, prettyname = "Workout", isDateBased = true)
public class JawboneUpWorkoutFacet extends JawboneUpGeoFacet {

    public String title;

    @Lob
    public String workoutDetails;

    public Integer sub_type;
    public Integer steps;
    public Integer duration;
    public Integer bg_active_time;
    public Double meters;
    public Double km;
    public Integer intensity;
    public Double calories;
    public Double bmr;
    public Double bg_calories;
    public Double bmr_calories;
    public String image;
    public String snapshot_image;
    public String route;

    @Override
    protected void makeFullTextIndexable() {}

    public JawboneUpWorkoutFacet(){super();}
    public JawboneUpWorkoutFacet(long apiKeyId){super(apiKeyId);}

}
