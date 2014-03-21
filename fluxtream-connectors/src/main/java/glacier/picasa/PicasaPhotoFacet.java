package glacier.picasa;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.domain.AbstractFacet;
import org.hibernate.search.annotations.Indexed;

@SuppressWarnings("serial")
@Entity(name="Facet_PicasaPhotoEntry")
@ObjectTypeSpec(name = "photo", value = 1, isImageType=true, prettyname = "Photos")
@NamedQueries({
	@NamedQuery(name = "picasa.photo.all", query = "SELECT facet FROM Facet_PicasaPhotoEntry facet WHERE facet.guestId=? ORDER BY facet.start ASC"),
	@NamedQuery(name = "picasa.photo.deleteAll", query = "DELETE FROM Facet_PicasaPhotoEntry facet WHERE facet.guestId=?"),
	@NamedQuery(name = "picasa.photo.between", query = "SELECT facet FROM Facet_PicasaPhotoEntry facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=? ORDER BY facet.start ASC"),
    @NamedQuery(name = "picasa.photo.newest", query = "SELECT facet FROM Facet_PicasaPhotoEntry facet WHERE facet.guestId=? ORDER BY facet.start DESC LIMIT 1")
})
@Indexed
public class PicasaPhotoFacet extends AbstractFacet implements Serializable {

	public String photoId;
	public String thumbnailUrl;
	public String photoUrl;
	public String title;

    /** @deprecated use the {@link AbstractFacet#comment} field instead */
	@Lob
	public String description;

    @Lob
	public String thumbnailsJson;
	
	public PicasaPhotoFacet(long apiKeyId) { super(apiKeyId); }

	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = "";
		if (title!=null)
			fullTextDescription += title;
		if (description!=null)
			fullTextDescription += " " + description;
	}
	
}
