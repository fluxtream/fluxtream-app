package com.fluxtream.connectors.up;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 07/02/14
 * Time: 11:22
 */
@Entity(name="Facet_JawboneUpMeal")
@ObjectTypeSpec(name = "meal", value = 16, prettyname = "Meal", isDateBased = true)
public class JawboneUpMealFacet extends JawboneUpGeoFacet {

    public String title;

    public double place_lat;
    public double place_lon;
    public int place_acc;
    public String place_name;
    public String tz;

    @Lob
    public String mealDetails;

    @Override
    protected void makeFullTextIndexable() {}

    public JawboneUpMealFacet(){super();}
    public JawboneUpMealFacet(long apiKeyId){super(apiKeyId);}

    @OneToMany(orphanRemoval = true, fetch= FetchType.EAGER, cascade= CascadeType.ALL)
    List<JawboneUpServingFacet> servings;

}
