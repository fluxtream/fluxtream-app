package com.fluxtream.connectors.picasa;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@SuppressWarnings("serial")
@Entity(name="Facet_PicasaPhotoEntry")
@ObjectTypeSpec(name = "photo", value = -1, isImageType=true, prettyname = "Photos")
@NamedQueries({
	@NamedQuery(name = "picasa.photo.all", query = "SELECT facet FROM Facet_PicasaPhotoEntry facet WHERE facet.guestId=? ORDER BY facet.start DESC"),
	@NamedQuery(name = "picasa.photo.deleteAll", query = "DELETE FROM Facet_PicasaPhotoEntry facet WHERE facet.guestId=?"),
	@NamedQuery(name = "picasa.photo.between", query = "SELECT facet FROM Facet_PicasaPhotoEntry facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class PicasaPhotoFacet extends AbstractFacet implements Serializable {

	public String photoId;
	public String thumbnailUrl;
	public String photoUrl;
	public String title;
	@Lob
	public String description;
	@Lob
	public String thumbnailsJson;
	
	public PicasaPhotoFacet() {}

	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = "";
		if (title!=null)
			fullTextDescription += title;
		if (description!=null)
			fullTextDescription += " " + description;
	}
	
}
