package com.fluxtream.connectors.fluxtream_capture;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Entity(name="Facet_FluxtreamCapturePhoto")
@ObjectTypeSpec(name = "photo", value = 1, isImageType=true, prettyname = "Photos")
@NamedQueries({
	@NamedQuery(name = "fluxtream_capture.photo.all", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? ORDER BY facet.start ASC"),
	@NamedQuery(name = "fluxtream_capture.photo.deleteAll", query = "DELETE FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=?"),
	@NamedQuery(name = "fluxtream_capture.photo.between", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? AND facet.start>=? AND facet.start<=? ORDER BY facet.start ASC"),
    @NamedQuery(name = "fluxtream_capture.photo.newest", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? ORDER BY facet.start DESC LIMIT 1")
})
@Indexed
public class FluxtreamCapturePhotoFacet extends AbstractFacet implements Serializable {
    public String title;
    public String hash;
    public String captureYYYYDD;
    public String latitude;
   	public String longitude;

    @Override
    protected void makeFullTextIndexable() {
        this.fullTextDescription = title;
    }
}
