package com.fluxtream.connectors.up;

import javax.persistence.Entity;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 07/02/14
 * Time: 11:34
 */
@Entity(name="Facet_JawboneUpWorkout")
@ObjectTypeSpec(name = "workout", value = 8, prettyname = "Workout", isDateBased = true)
public class JawboneUpWorkoutFacet extends JawboneUpGeoFacet {

    public String title;

    @Override
    protected void makeFullTextIndexable() {}

    public JawboneUpWorkoutFacet(){super();}
    public JawboneUpWorkoutFacet(long apiKeyId){super(apiKeyId);}

}
