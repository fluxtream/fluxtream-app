package com.fluxtream.connectors.fluxtream_capture;

import java.awt.Dimension;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import com.drew.imaging.ImageProcessingException;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Geolocation;
import com.fluxtream.utils.HashUtils;
import com.fluxtream.utils.ImageUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class FluxtreamCapturePhoto {
    private static final Logger LOG = Logger.getLogger(FluxtreamCapturePhoto.class);

    private static final int THUMBNAIL_SMALL_MAX_SIDE_LENGTH_IN_PIXELS = 150;
    private static final int THUMBNAIL_LARGE_MAX_SIDE_LENGTH_IN_PIXELS = 300;
    private static final String KEY_VALUE_STORE_KEY_PART_DELIMITER = ".";
    private static final String KEY_VALUE_STORE_FILENAME_PART_DELIMITER = "_";
    private static final String CONNECTOR_PRETTY_NAME = Connector.getConnector(FluxtreamCaptureUpdater.CONNECTOR_NAME).prettyName();
    private static final String OBJECT_TYPE_NAME = "photo";

    @NotNull
    public static String createPhotoStoreKey(final long guestId,
                                             @NotNull final String captureYYYYDDD,
                                             final long captureTimeMillisUtc,
                                             @NotNull final String photoHash) {
        return guestId + KEY_VALUE_STORE_KEY_PART_DELIMITER +
               CONNECTOR_PRETTY_NAME + KEY_VALUE_STORE_KEY_PART_DELIMITER +
               OBJECT_TYPE_NAME + KEY_VALUE_STORE_KEY_PART_DELIMITER +
               captureYYYYDDD + KEY_VALUE_STORE_KEY_PART_DELIMITER +
               captureTimeMillisUtc + KEY_VALUE_STORE_FILENAME_PART_DELIMITER +
               photoHash;
    }

    private final long guestId;

    @NotNull
    private final byte[] photoBytes;

    private final long captureTimeMillisUtc;

    @NotNull
    private final String captureYYYYDDD;

    @NotNull
    private final String photoHash;

    @NotNull
    private final String photoStoreKey;

    @NotNull
    private final byte[] thumbnailSmall;

    @NotNull
    private final byte[] thumbnailLarge;

    @NotNull
    private final Dimension thumbnailSmallSize;

    @NotNull
    private final Dimension thumbnailLargeSize;

    @NotNull
    private final ImageUtils.Orientation orientation;

    @Nullable
    private final Geolocation geolocation;

    FluxtreamCapturePhoto(final long guestId, @NotNull final byte[] photoBytes, final long captureTimeMillisUtc) throws IllegalArgumentException, NoSuchAlgorithmException, IOException, FluxtreamCapturePhotoStore.UnsupportedImageFormatException {

        if (!ImageUtils.isSupportedImage(photoBytes)) {
            throw new FluxtreamCapturePhotoStore.UnsupportedImageFormatException("The photoBytes do not contain a supported image format");
        }

        if (captureTimeMillisUtc < 0) {
            throw new IllegalArgumentException("The captureTimeMillisUtc must be non-negative");
        }

        this.guestId = guestId;
        this.photoBytes = photoBytes;
        this.captureTimeMillisUtc = captureTimeMillisUtc;

        final DateTime captureTime = new DateTime(captureTimeMillisUtc, DateTimeZone.UTC);
        final String year = String.valueOf(captureTime.getYear());
        final String dayOfYear = String.format("%03d", captureTime.getDayOfYear()); // pad with zeros so that it's always 3 characters
        captureYYYYDDD = year + dayOfYear;
        this.photoHash = HashUtils.computeSha256Hash(photoBytes);
        photoStoreKey = createPhotoStoreKey(guestId, captureYYYYDDD, captureTimeMillisUtc, photoHash);

        // Create the thumbnails: do so by creating the large one first, and then creating the smaller
        // one from the larger--this should be faster than creating each from the original image
        final ImageUtils.Thumbnail thumbnailLargeImage = ImageUtils.createJpegThumbnail(photoBytes, THUMBNAIL_LARGE_MAX_SIDE_LENGTH_IN_PIXELS);
        if (thumbnailLargeImage == null) {
            throw new IOException("Failed to create thumbnails");
        }

        final ImageUtils.Thumbnail thumbnailSmallImage = ImageUtils.createJpegThumbnail(thumbnailLargeImage.getBytes(), THUMBNAIL_SMALL_MAX_SIDE_LENGTH_IN_PIXELS);
        if (thumbnailSmallImage == null) {
            throw new IOException("Failed to create thumbnails");
        }

        thumbnailSmall = thumbnailSmallImage.getBytes();
        thumbnailLarge = thumbnailLargeImage.getBytes();

        thumbnailSmallSize = new Dimension(thumbnailSmallImage.getWidth(), thumbnailSmallImage.getHeight());
        thumbnailLargeSize = new Dimension(thumbnailLargeImage.getWidth(), thumbnailLargeImage.getHeight());

        // get the image orientation, and default to ORIENTATION_1 if unspecified
        ImageUtils.Orientation orientationTemp;
        try {
            orientationTemp = ImageUtils.getOrientation(photoBytes);
        }
        catch (Exception e) {
            LOG.error("Exception while trying to read the orientation data for user [" + guestId + "] photo [" + photoStoreKey + "]");
            orientationTemp = null;
        }
        orientation = (orientationTemp == null) ? ImageUtils.Orientation.ORIENTATION_1 : orientationTemp;

        Geolocation geolocationTemp;
        try {
            geolocationTemp = ImageUtils.getGeolocation(photoBytes);
        }
        catch (ImageProcessingException e) {
            LOG.error("ImageProcessingException while trying to read the geolocation data for user [" + guestId + "] photo [" + photoStoreKey + "]");
            geolocationTemp = null;
        }
        geolocation = geolocationTemp;
    }

    public long getGuestId() {
        return guestId;
    }

    @NotNull
    public byte[] getPhotoBytes() {
        return photoBytes;
    }

    @NotNull
    public String getPhotoHash() {
        return photoHash;
    }

    public long getCaptureTimeMillisUtc() {
        return captureTimeMillisUtc;
    }

    @NotNull
    public String getCaptureYYYYDDD() {
        return captureYYYYDDD;
    }

    @NotNull
    public String getPhotoStoreKey() {
        return photoStoreKey;
    }

    @NotNull
    public byte[] getThumbnailSmall() {
        return thumbnailSmall;
    }

    @NotNull
    public byte[] getThumbnailLarge() {
        return thumbnailLarge;
    }

    @NotNull
    public Dimension getThumbnailSmallSize() {
        return thumbnailSmallSize;
    }

    @NotNull
    public Dimension getThumbnailLargeSize() {
        return thumbnailLargeSize;
    }

    @NotNull
    public ImageUtils.Orientation getOrientation() {
        return orientation;
    }

    @Nullable
    public Geolocation getGeolocation() {
        return geolocation;
    }
}
