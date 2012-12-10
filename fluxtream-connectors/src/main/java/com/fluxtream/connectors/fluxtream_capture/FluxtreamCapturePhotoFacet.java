package com.fluxtream.connectors.fluxtream_capture;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.utils.HashUtils;
import org.hibernate.search.annotations.Indexed;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Entity(name = "Facet_FluxtreamCapturePhoto")
@ObjectTypeSpec(name = "photo", value = 1, isImageType = true, prettyname = "Photos")
@NamedQueries({
    @NamedQuery(name = "fluxtream_capture.photo.all", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? ORDER BY facet.start ASC"),
    @NamedQuery(name = "fluxtream_capture.photo.deleteAll", query = "DELETE FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=?"),
    @NamedQuery(name = "fluxtream_capture.photo.between", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? AND facet.start>=? AND facet.start<=? ORDER BY facet.start ASC"),
    @NamedQuery(name = "fluxtream_capture.photo.newest", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? ORDER BY facet.start DESC LIMIT 1")
})
@Indexed
public class FluxtreamCapturePhotoFacet extends AbstractFacet implements Serializable {

    public String hash;
    public String title;
    public String captureYYYYDDD;
    public String latitude;
    public String longitude;

    public FluxtreamCapturePhotoFacet() {

    }

    public FluxtreamCapturePhotoFacet(long guestId,
                                      @NotNull final byte[] photoBytes,
                                      final long captureTimeMillis,
                                      @NotNull final String hash) {
        this.guestId = guestId;
        timeUpdated = System.currentTimeMillis();
        start = captureTimeMillis;
        end = captureTimeMillis;
        this.hash = hash;
        final Connector connector = Connector.getConnector(FluxtreamCaptureUpdater.CONNECTOR_NAME);
        this.api = connector.value();
        this.objectType = ObjectType.getObjectType(connector, "photo").value();

        final DateTime captureTime = new DateTime(captureTimeMillis, DateTimeZone.UTC);
        final String year = String.valueOf(captureTime.getYear());
        final String dayOfYear = String.format("%03d", captureTime.getDayOfYear()); // pad with zeros so that it's always 3 characters
        captureYYYYDDD = year + dayOfYear;

        //TODO: create thumbnails from photoBytes
    }

    /**
     * Creates a hash for the given photo and returns it as a {@link String} of hex bytes. Guaranteed to not return
     * <code>null</code>, but might throw a {@link NoSuchAlgorithmException} if the SHA-256 algorithm is not available.
     *
     * @throws NoSuchAlgorithmException
     */
    @NotNull
    public static String computeHash(@NotNull final byte[] photoBytes) throws NoSuchAlgorithmException {
        return HashUtils.computeSha256Hash(photoBytes);
    }

    @Override
    protected void makeFullTextIndexable() {
        this.fullTextDescription = title;
    }
}
