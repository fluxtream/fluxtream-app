package com.fluxtream.connectors.fluxtream_capture;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import com.fluxtream.connectors.Connector;
import com.fluxtream.utils.HashUtils;
import com.fluxtream.utils.ImageUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class FluxtreamCapturePhoto {
    private static final int THUMBNAIL_SMALL_MAX_SIDE_LENGTH_IN_PIXELS = 150;
    private static final int THUMBNAIL_LARGE_MAX_SIDE_LENGTH_IN_PIXELS = 300;
    private static final String KEY_VALUE_STORE_KEY_PART_DELIMITER = ".";
    private static final String KEY_VALUE_STORE_FILENAME_PART_DELIMITER = "_";
    private static final String CONNECTOR_PRETTY_NAME = Connector.getConnector(FluxtreamCaptureUpdater.CONNECTOR_NAME).prettyName();
    private static final String OBJECT_TYPE_NAME = "photo";

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

    FluxtreamCapturePhoto(final long guestId, @NotNull final byte[] photoBytes, final long captureTimeMillisUtc) throws IllegalArgumentException, NoSuchAlgorithmException, IOException {

        if (!ImageUtils.isImage(photoBytes)) {
            throw new IllegalArgumentException("The photoBytes do not contain a supported image type");
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
        photoStoreKey = guestId + KEY_VALUE_STORE_KEY_PART_DELIMITER +
                        CONNECTOR_PRETTY_NAME + KEY_VALUE_STORE_KEY_PART_DELIMITER +
                        OBJECT_TYPE_NAME + KEY_VALUE_STORE_KEY_PART_DELIMITER +
                        captureYYYYDDD + KEY_VALUE_STORE_KEY_PART_DELIMITER +
                        photoHash + KEY_VALUE_STORE_FILENAME_PART_DELIMITER +
                        String.valueOf(captureTimeMillisUtc);

        final byte[] thumbnailSmallTemp = ImageUtils.createThumbnail(photoBytes, THUMBNAIL_SMALL_MAX_SIDE_LENGTH_IN_PIXELS);
        final byte[] thumbnailLargeTemp = ImageUtils.createThumbnail(photoBytes, THUMBNAIL_LARGE_MAX_SIDE_LENGTH_IN_PIXELS);

        if (thumbnailSmallTemp == null || thumbnailLargeTemp == null) {
            throw new IOException("Failed to create thumbnails");
        }

        thumbnailSmall = thumbnailSmallTemp;
        thumbnailLarge = thumbnailLargeTemp;

        // TODO: extract Lat/Long
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
}
