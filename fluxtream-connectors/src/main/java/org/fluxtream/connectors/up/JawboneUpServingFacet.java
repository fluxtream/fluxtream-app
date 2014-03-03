package org.fluxtream.connectors.up;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.connectors.location.LocationFacet;

/**
 * User: candide
 * Date: 07/02/14
 * Time: 13:40
 */
@Entity(name="Facet_JawboneUpServing")
@ObjectTypeSpec(name = "serving", value = 32, isImageType=true, prettyname = "Serving",
                isDateBased = true, locationFacetSource = LocationFacet.Source.JAWBONE_UP)
public class JawboneUpServingFacet extends JawboneUpFacet {

    public String image;

    @Lob
    public String servingDetails;

    @Override
    protected void makeFullTextIndexable() {}


    @ManyToOne(fetch= FetchType.EAGER, cascade= CascadeType.ALL)
    public JawboneUpMealFacet meal;

    public JawboneUpServingFacet(){super();}
    public JawboneUpServingFacet(long apiKeyId){super(apiKeyId);}


}
