package org.fluxtream.connectors.fluxtream_capture;

import java.awt.Dimension;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import com.drew.imaging.ImageProcessingException;
import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.Geolocation;
import org.fluxtream.images.Image;
import org.fluxtream.images.ImageOrientation;
import org.fluxtream.images.ImageType;
import org.fluxtream.utils.HashUtils;
import org.fluxtream.utils.ImageUtils;
import org.fluxtream.aspects.FlxLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class FluxtreamCapturePhoto {

    private static final FlxLogger logger = FlxLogger.getLogger(FluxtreamCapturePhoto.class);

    public static class PhotoUploadMetadata {
        private double capture_time_secs_utc = -1;
        @Nullable
        private String comment;
        @Nullable
        private String tags;

        public boolean isValid() {
            return capture_time_secs_utc >= 0;
        }

        public long getCaptureTimeMillisUtc() {
            // convert the capture time from seconds to milliseconds
            return (long)(capture_time_secs_utc * 1000);
        }

        public double getCaptureTimeSecsUtc() {
            return capture_time_secs_utc;
        }

        @Nullable
        public String getComment() {
            return comment;
        }

        @Nullable
        public String getTags() {
            return tags;
        }
    }

    private static final int THUMBNAIL_0_MAX_SIDE_LENGTH_IN_PIXELS = 150;
    private static final int THUMBNAIL_1_MAX_SIDE_LENGTH_IN_PIXELS = 300;
    private static final int THUMBNAIL_2_MAX_SIDE_LENGTH_IN_PIXELS = 500;
    private static final String KEY_VALUE_STORE_KEY_PART_DELIMITER = ".";
    private static final String KEY_VALUE_STORE_FILENAME_PART_DELIMITER = "_";
    private static final String CONNECTOR_PRETTY_NAME = Connector.getConnector("fluxtream_capture").prettyName();
    private static final String OBJECT_TYPE_NAME = "photo";

    @NotNull
    public static String createPhotoStoreKey(final long guestId, @NotNull final String captureYYYYDDD, final long captureTimeMillisUtc, @NotNull final String photoHash) {
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
    private final byte[] thumbnail0;

    @NotNull
    private final byte[] thumbnail1;

    @NotNull
    private final byte[] thumbnail2;

    @NotNull
    private final Dimension thumbnail0Size;

    @NotNull
    private final Dimension thumbnail1Size;

    @NotNull
    private final Dimension thumbnail2Size;

    @NotNull
    private final ImageOrientation orientation;

    @NotNull
    private final ImageType imageType;

    @Nullable
    private final Geolocation geolocation;

    @Nullable
    private final String tags;

    @Nullable
    private final String comment;

    FluxtreamCapturePhoto(final long guestId, @NotNull final byte[] photoBytes, @NotNull final PhotoUploadMetadata photoUploadMetadata) throws IllegalArgumentException, NoSuchAlgorithmException, IOException, FluxtreamCapturePhotoStore.UnsupportedImageFormatException {

        // Get the image type.  If this is null, then it's not a supported type.
        final ImageType tempImageType = ImageUtils.getImageType(photoBytes);
        if (tempImageType == null) {
            throw new FluxtreamCapturePhotoStore.UnsupportedImageFormatException("The photoBytes do not contain a supported image format");
        }
        imageType = tempImageType;

        if (photoUploadMetadata.getCaptureTimeSecsUtc() < 0) {
            throw new IllegalArgumentException("The captureTimeMillisUtc must be non-negative");
        }

        this.guestId = guestId;
        this.photoBytes = photoBytes;
        this.captureTimeMillisUtc = photoUploadMetadata.getCaptureTimeMillisUtc();
        this.tags = photoUploadMetadata.getTags();
        this.comment = photoUploadMetadata.getComment();

        final DateTime captureTime = new DateTime(captureTimeMillisUtc, DateTimeZone.UTC);
        final String year = String.valueOf(captureTime.getYear());
        final String dayOfYear = String.format("%03d", captureTime.getDayOfYear()); // pad with zeros so that it's always 3 characters
        captureYYYYDDD = year + dayOfYear;
        this.photoHash = HashUtils.computeSha256Hash(photoBytes);
        photoStoreKey = createPhotoStoreKey(guestId, captureYYYYDDD, captureTimeMillisUtc, photoHash);

        // Create the thumbnails: do so by creating the largest one first, and then creating the smaller
        // ones from the larger--this should be faster than creating each from the original image
        final Image thumbnail2Image = ImageUtils.createJpegThumbnail(photoBytes, THUMBNAIL_2_MAX_SIDE_LENGTH_IN_PIXELS);
        if (thumbnail2Image == null) {
            throw new IOException("Failed to create thumbnails");
        }

        final Image thumbnail1Image = ImageUtils.createJpegThumbnail(thumbnail2Image.getBytes(), THUMBNAIL_1_MAX_SIDE_LENGTH_IN_PIXELS);
        if (thumbnail1Image == null) {
            throw new IOException("Failed to create thumbnails");
        }

        final Image thumbnail0Image = ImageUtils.createJpegThumbnail(thumbnail1Image.getBytes(), THUMBNAIL_0_MAX_SIDE_LENGTH_IN_PIXELS);
        if (thumbnail0Image == null) {
            throw new IOException("Failed to create thumbnails");
        }

        thumbnail0 = thumbnail0Image.getBytes();
        thumbnail1 = thumbnail1Image.getBytes();
        thumbnail2 = thumbnail2Image.getBytes();

        thumbnail0Size = new Dimension(thumbnail0Image.getWidth(), thumbnail0Image.getHeight());
        thumbnail1Size = new Dimension(thumbnail1Image.getWidth(), thumbnail1Image.getHeight());
        thumbnail2Size = new Dimension(thumbnail2Image.getWidth(), thumbnail2Image.getHeight());

        // get the image orientation, and default to ORIENTATION_1 if unspecified
        ImageOrientation orientationTemp;
        try {
            orientationTemp = ImageUtils.getOrientation(photoBytes);
        }
        catch (Exception e) {
            logger.error("Exception while trying to read the orientation data for user [" + guestId + "] photo [" + photoStoreKey + "]");
            orientationTemp = null;
        }
        orientation = (orientationTemp == null) ? ImageOrientation.ORIENTATION_1 : orientationTemp;

        Geolocation geolocationTemp;
        try {
            geolocationTemp = ImageUtils.getGeolocation(photoBytes);
        }
        catch (ImageProcessingException e) {
            logger.error("ImageProcessingException while trying to read the geolocation data for user [" + guestId + "] photo [" + photoStoreKey + "]");
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
    public byte[] getThumbnail0() {
        return thumbnail0;
    }

    @NotNull
    public byte[] getThumbnail1() {
        return thumbnail1;
    }

    @NotNull
    public byte[] getThumbnail2() {
        return thumbnail2;
    }

    @NotNull
    public Dimension getThumbnail0Size() {
        return thumbnail0Size;
    }

    @NotNull
    public Dimension getThumbnail1Size() {
        return thumbnail1Size;
    }

    @NotNull
    public Dimension getThumbnail2Size() {
        return thumbnail2Size;
    }

    @NotNull
    public ImageOrientation getOrientation() {
        return orientation;
    }

    @NotNull
    public ImageType getImageType() {
        return imageType;
    }

    @Nullable
    public Geolocation getGeolocation() {
        return geolocation;
    }

    @Nullable
    public String getTags() {
        return tags;
    }

    @Nullable
    public String getComment() {
        return comment;
    }
}
