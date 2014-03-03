package org.fluxtream.connectors.flickr;

import javax.persistence.Entity;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.connectors.location.LocationFacet;
import org.fluxtream.domain.AbstractLocalTimeFacet;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_FlickrPhoto")
@ObjectTypeSpec(name = "photo", value = 1, isImageType=true, prettyname = "Photos", isDateBased = true, locationFacetSource = LocationFacet.Source.FLICKR)
@Indexed
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