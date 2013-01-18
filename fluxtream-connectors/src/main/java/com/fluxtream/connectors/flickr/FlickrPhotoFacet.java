package com.fluxtream.connectors.flickr;

import javax.persistence.Entity;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFloatingTimeZoneFacet;
import org.hibernate.search.annotations.Indexed;

@Entity(name="Facet_FlickrPhoto")
@ObjectTypeSpec(name = "photo", value = 1, isImageType=true, prettyname = "Photos", isDateBased = true)
@Indexed
public class FlickrPhotoFacet extends AbstractFloatingTimeZoneFacet {

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
	public String latitude;
	public String longitude;
	public int accuracy;
	
	public FlickrPhotoFacet() {}

	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = title;
	}
}