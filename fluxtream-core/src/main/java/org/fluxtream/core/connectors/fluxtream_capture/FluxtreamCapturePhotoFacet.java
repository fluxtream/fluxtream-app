package org.fluxtream.core.connectors.fluxtream_capture;

import java.awt.Dimension;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.Geolocation;
import org.fluxtream.core.domain.Tag;
import org.fluxtream.core.images.ImageOrientation;
import org.hibernate.search.annotations.Indexed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Entity(name = "Facet_FluxtreamCapturePhoto")
@ObjectTypeSpec(name = "photo", value = 1, isImageType = true, prettyname = "Photos")
@NamedQueries({
    @NamedQuery(name = "fluxtream_capture.photo.all", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? ORDER BY facet.start ASC"),
    @NamedQuery(name = "fluxtream_capture.photo.newest", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? ORDER BY facet.start DESC LIMIT 1"),
    @NamedQuery(name = "fluxtream_capture.photo.byId", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? AND facet.id=?")
})
@Indexed
public class FluxtreamCapturePhotoFacet extends AbstractFacet implements Serializable, Geolocation {

    public static final int NUM_THUMBNAILS = 3;

    private String hash;
    private String title;
    private String captureYYYYDDD;
    private String imageType;

    @Lob
    private byte[] thumbnail0;

    @Lob
    private byte[] thumbnail1;

    @Lob
    private byte[] thumbnail2;

    private int thumbnail0Width;
    private int thumbnail0Height;
    private int thumbnail1Width;
    private int thumbnail1Height;
    private int thumbnail2Width;
    private int thumbnail2Height;

    private int orientation;

    private Double latitude;
    private Double longitude;
    private Float heading;
    private String headingRef;
    private Float altitude;
    private Integer altitudeRef;
    private Float gpsPrecision;
    private String gpsDatestamp;
    private String gpsTimestamp;

    public FluxtreamCapturePhotoFacet() {
        super();
    }

    @SuppressWarnings("UnusedDeclaration")
    public FluxtreamCapturePhotoFacet(Long apiKeyId) {
        super(apiKeyId);
    }

    public FluxtreamCapturePhotoFacet(@NotNull final FluxtreamCapturePhoto photo, final Long apiKeyId) {
        super(apiKeyId);
        guestId = photo.getGuestId();
        timeUpdated = System.currentTimeMillis();
        start = photo.getCaptureTimeMillisUtc();
        end = start;
        hash = photo.getPhotoHash();

        final Connector connector = Connector.getConnector("fluxtream_capture");
        this.api = connector.value();
        this.objectType = ObjectType.getObjectType(connector, "photo").value();

        captureYYYYDDD = photo.getCaptureYYYYDDD();

        imageType = photo.getImageType().getFileExtension();

        thumbnail0 = photo.getThumbnail0();
        thumbnail1 = photo.getThumbnail1();
        thumbnail2 = photo.getThumbnail2();

        final Dimension thumbnail0Size = photo.getThumbnail0Size();
        final Dimension thumbnail1Size = photo.getThumbnail1Size();
        final Dimension thumbnail2Size = photo.getThumbnail2Size();

        thumbnail0Width = thumbnail0Size.width;
        thumbnail0Height = thumbnail0Size.height;
        thumbnail1Width =  thumbnail1Size.width;
        thumbnail1Height = thumbnail1Size.height;
        thumbnail2Width =  thumbnail2Size.width;
        thumbnail2Height = thumbnail2Size.height;

        orientation = photo.getOrientation().getId();

        this.addTags(photo.getTags(), Tag.COMMA_DELIMITER);
        this.comment = photo.getComment();

        final Geolocation geolocation = photo.getGeolocation();
        if (geolocation != null) {
            latitude = geolocation.getLatitude();
            longitude = geolocation.getLongitude();
            heading = geolocation.getHeading();
            headingRef = geolocation.getHeadingRef();
            altitude = geolocation.getAltitude();
            altitudeRef = geolocation.getAltitudeRef();
            gpsPrecision = geolocation.getGpsPrecision();
            gpsDatestamp = geolocation.getGpsDatestamp();
            gpsTimestamp = geolocation.getGpsTimestamp();
        }
    }

    @Override
    protected void makeFullTextIndexable() {
        this.fullTextDescription = title;
    }

    public long getGuestId() {
        return guestId;
    }

    public String getHash() {
        return hash;
    }

    public String getTitle() {
        return title;
    }

    public String getCaptureYYYYDDD() {
        return captureYYYYDDD;
    }

    public String getImageType() {
        return imageType;
    }

    /** Returns the thumbnail associated with the given <code>thumbnailIndex</code>, or thumbnail0 if no such index exists. */
    public byte[] getThumbnail(final int thumbnailIndex) {
        if (thumbnailIndex == 1) {
            return thumbnail1;
        } else if (thumbnailIndex == 2) {
            return thumbnail2;
        }

        return thumbnail0;
    }

    @Nullable
    public Dimension getThumbnailSize(final int thumbnailIndex) {
        switch (thumbnailIndex) {
            case 0:
                return new Dimension(thumbnail0Width, thumbnail0Height);
            case 1:
                return new Dimension(thumbnail1Width, thumbnail1Height);
            case 2:
                return new Dimension(thumbnail2Width, thumbnail2Height);
        }
        return null;
    }

    @Nullable
    public ImageOrientation getOrientation() {
        return ImageOrientation.findById(orientation);
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Float getHeading() {
        return heading;
    }

    public String getHeadingRef() {
        return headingRef;
    }

    public Float getAltitude() {
        return altitude;
    }

    public Integer getAltitudeRef() {
        return altitudeRef;
    }

    public Float getGpsPrecision() {
        return gpsPrecision;
    }

    public String getGpsDatestamp() {
        return gpsDatestamp;
    }

    public String getGpsTimestamp() {
        return gpsTimestamp;
    }

    @Nullable
    public String getPhotoStoreKey() {
        if (guestId != 0 && captureYYYYDDD != null && start != 0 && hash != null) {
            return FluxtreamCapturePhoto.createPhotoStoreKey(guestId, captureYYYYDDD, start, hash);
        }
        return null;
    }
}
