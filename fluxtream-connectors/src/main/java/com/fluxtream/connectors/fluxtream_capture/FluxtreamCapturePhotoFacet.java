package com.fluxtream.connectors.fluxtream_capture;

import java.awt.Dimension;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.Geolocation;
import com.fluxtream.utils.ImageUtils;
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
    @NamedQuery(name = "fluxtream_capture.photo.deleteAll", query = "DELETE FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=?"),
    @NamedQuery(name = "fluxtream_capture.photo.between", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? AND facet.start>=? AND facet.start<=? ORDER BY facet.start ASC"),
    @NamedQuery(name = "fluxtream_capture.photo.newest", query = "SELECT facet FROM Facet_FluxtreamCapturePhoto facet WHERE facet.guestId=? ORDER BY facet.start DESC LIMIT 1")
})
@Indexed
public class FluxtreamCapturePhotoFacet extends AbstractFacet implements Serializable, Geolocation {

    private String hash;
    private String title;
    private String captureYYYYDDD;

    @Lob
    private byte[] thumbnailSmall;

    @Lob
    private byte[] thumbnailLarge;

    private int thumbnailSmallWidth;
    private int thumbnailSmallHeight;
    private int thumbnailLargeWidth;
    private int thumbnailLargeHeight;

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

    @SuppressWarnings("UnusedDeclaration")
    public FluxtreamCapturePhotoFacet() {
        // need this for Hibernate
    }

    public FluxtreamCapturePhotoFacet(@NotNull final FluxtreamCapturePhoto photo) {
        guestId = photo.getGuestId();
        timeUpdated = System.currentTimeMillis();
        start = photo.getCaptureTimeMillisUtc();
        end = start;
        hash = photo.getPhotoHash();

        final Connector connector = Connector.getConnector(FluxtreamCaptureUpdater.CONNECTOR_NAME);
        this.api = connector.value();
        this.objectType = ObjectType.getObjectType(connector, "photo").value();

        captureYYYYDDD = photo.getCaptureYYYYDDD();

        thumbnailSmall = photo.getThumbnailSmall();
        thumbnailLarge = photo.getThumbnailLarge();

        final Dimension thumbnailSmallSize = photo.getThumbnailSmallSize();
        final Dimension thumbnailLargeSize = photo.getThumbnailLargeSize();

        thumbnailSmallWidth = thumbnailSmallSize.width;
        thumbnailSmallHeight = thumbnailSmallSize.height;
        thumbnailLargeWidth =  thumbnailLargeSize.width;
        thumbnailLargeHeight = thumbnailLargeSize.height;

        orientation = photo.getOrientation().getId();

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

    public String getHash() {
        return hash;
    }

    public String getTitle() {
        return title;
    }

    public String getCaptureYYYYDDD() {
        return captureYYYYDDD;
    }

    public byte[] getThumbnailSmall() {
        return thumbnailSmall;
    }

    public byte[] getThumbnailLarge() {
        return thumbnailLarge;
    }

    @NotNull
    public Dimension getThumbnailSmallSize() {
        return new Dimension(thumbnailSmallWidth, thumbnailSmallHeight);
    }

    @NotNull
    public Dimension getThumbnailLargeSize() {
        return new Dimension(thumbnailLargeWidth, thumbnailLargeHeight);
    }

    @Nullable
    public ImageUtils.Orientation getOrientation() {
        return ImageUtils.Orientation.findById(orientation);
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
}
