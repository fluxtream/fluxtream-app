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

    @Lob
    public String mealDetails;

    @Override
    protected void makeFullTextIndexable() {}

    public JawboneUpMealFacet(){super();}
    public JawboneUpMealFacet(long apiKeyId){super(apiKeyId);}

    @OneToMany(mappedBy = "meal", orphanRemoval = true, fetch= FetchType.EAGER, cascade= CascadeType.ALL)
    List<JawboneUpServingFacet> servings;

}
