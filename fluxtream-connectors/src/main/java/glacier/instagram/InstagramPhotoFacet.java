package glacier.instagram;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;

@Entity(name="Facet_InstagramPhoto")
@ObjectTypeSpec(name = "photo", value = -1, isImageType=true, prettyname = "Photos")
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
	
	public InstagramPhotoFacet(long apiKeyId) { super(apiKeyId); }

	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = caption;
	}
}