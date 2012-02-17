package com.fluxtream.connectors.instagram;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_InstagramPhoto")
@ObjectTypeSpec(name = "photo", value = -1, isImageType=true, prettyname = "Photos")
@NamedQueries({
		@NamedQuery(name = "instagram.photo.deleteAll", query = "DELETE FROM Facet_InstagramPhoto facet WHERE facet.guestId=?"),
		@NamedQuery(name = "instagram.photo.between", query = "SELECT facet FROM Facet_InstagramPhoto facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class InstagramPhotoFacet extends AbstractFacet {

	public String instagramId;
	public String filter;
	public String lowResolutionUrl;
	public String thumbnailUrl;
	public String standardResolutionUrl;
	
	public double latitude;
	public double longitude;
	public String locationName;
	public String locationId;
	
	public int lowResolutionWidth;
	public int lowResolutionHeight;
	public int thumbnailWidth;
	public int thumbnailHeight;
	public int standardResolutionWidth;
	public int standardResolutionHeight;
	
	public String link;
	public String caption;
	
	public InstagramPhotoFacet() {}

	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = caption;
	}
}