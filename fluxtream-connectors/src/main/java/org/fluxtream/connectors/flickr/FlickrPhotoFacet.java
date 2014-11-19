package org.fluxtream.connectors.flickr;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;

import javax.persistence.Entity;

@Entity(name="Facet_FlickrPhoto")
@ObjectTypeSpec(name = "photo", value = 1, isImageType=true, prettyname = "Photos", isDateBased = true, locationFacetSource = LocationFacet.Source.FLICKR)
public class FlickrPhotoFacet extends AbstractLocalTimeFacet {

	public String flickrId;
	public String owner;
	public String secret;
	public String server;
	public String farm;
	public String title;
	public boolean ispublic;
	public boolean isfriend;
	public boolean isfamily;
	public long datetaken;
	public long dateupload;
    public Long dateupdated;
	public Float latitude;
	public Float longitude;
	public int accuracy;

    public FlickrPhotoFacet() {super();}

	public FlickrPhotoFacet(long apiKeyId) {super(apiKeyId);}

	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = title;
	}
}