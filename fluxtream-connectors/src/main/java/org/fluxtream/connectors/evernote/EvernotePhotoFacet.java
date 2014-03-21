package org.fluxtream.connectors.evernote;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.connectors.location.LocationFacet;

/**
 * User: candide
 * Date: 03/01/14
 * Time: 15:34
 */
@Entity(name="Facet_EvernotePhoto")
@ObjectTypeSpec(name = "photo", value = 32, isImageType=true, prettyname = "Photos", locationFacetSource = LocationFacet.Source.EVERNOTE)
public class EvernotePhotoFacet extends EvernoteFacet {

    @Override
    protected void makeFullTextIndexable() {}

    public EvernotePhotoFacet() {super();}

    public EvernotePhotoFacet(final long apiKeyId) {super(apiKeyId);}

    @OneToOne(fetch= FetchType.EAGER, targetEntity=EvernoteResourceFacet.class, cascade=CascadeType.ALL)
    public EvernoteResourceFacet resourceFacet;

}
