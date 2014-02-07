package com.fluxtream.connectors.up;

import javax.persistence.Entity;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.connectors.location.LocationFacet;

/**
 * User: candide
 * Date: 07/02/14
 * Time: 13:40
 */
@Entity(name="Facet_JawboneUpServingPhoto")
@ObjectTypeSpec(name = "servingPhoto", value = 32, isImageType=true, prettyname = "Serving Photos", locationFacetSource = LocationFacet.Source.JAWBONE_UP)
public class JawboneUpServingFacet extends JawboneUpFacet {

    @Override
    protected void makeFullTextIndexable() {}

    public JawboneUpServingFacet(){super();}
    public JawboneUpServingFacet(long apiKeyId){super(apiKeyId);}


}
