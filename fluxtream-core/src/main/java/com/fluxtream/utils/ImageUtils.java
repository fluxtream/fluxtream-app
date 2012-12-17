package com.fluxtream.utils;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDescriptor;
import com.drew.metadata.exif.GpsDirectory;
import com.fluxtream.domain.Geolocation;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.imgscalr.Scalr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * <code>ImageUtils</code> provides helpful methods for dealing with images.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class ImageUtils {
    private static final Logger LOG = Logger.getLogger(ImageUtils.class);

    /**
     * An enum for recording image orientation and what operation(s) needs to applied in order to transform it.
     * There are eight possible orientations, shown here (taken from http://sylvana.net/jpegcrop/exif_orientation.html):
     * <pre>
     *     1        2       3      4         5            6           7          8
     *
     *   888888  888888      88  88      8888888888  88                  88  8888888888
     *   88          88      88  88      88  88      88  88          88  88      88  88
     *   8888      8888    8888  8888    88          8888888888  8888888888          88
     *   88          88      88  88
     *   88          88  888888  888888
     * </pre>
     */
    public static enum Orientation {
        ORIENTATION_1(1, null, null),
        ORIENTATION_2(2, null, Scalr.Rotation.FLIP_HORZ),
        ORIENTATION_3(3, Scalr.Rotation.CW_180, null),
        ORIENTATION_4(4, null, Scalr.Rotation.FLIP_VERT),
        ORIENTATION_5(5, Scalr.Rotation.CW_90, Scalr.Rotation.FLIP_HORZ),
        ORIENTATION_6(6, Scalr.Rotation.CW_90, null),
        ORIENTATION_7(7, Scalr.Rotation.CW_270, Scalr.Rotation.FLIP_HORZ),
        ORIENTATION_8(8, Scalr.Rotation.CW_270, null);

        private static final Map<Integer, Orientation> ID_TO_BAUD_RATE_MAP;

        static {
            final Map<Integer, Orientation> idToOrienationMap = new HashMap<Integer, Orientation>();
            for (final Orientation orientation : Orientation.values()) {
                idToOrienationMap.put(orientation.getId(), orientation);
            }
            ID_TO_BAUD_RATE_MAP = Collections.unmodifiableMap(idToOrienationMap);
        }

        @Nullable
        public static Orientation findById(final int id) {
            return ID_TO_BAUD_RATE_MAP.get(id);
        }

        /**
         * Tries to read the EXIF orientation data in the image in the {@link InputStream}.  Returns <code>null</code>
         * if not found or if an error occurs.
         */
        @Nullable
        public static Orientation getOrientation(@Nullable final InputStream inputStream) {
            if (inputStream != null) {
                try {
                    return getOrientation(ImageMetadataReader.readMetadata(new BufferedInputStream(inputStream), true));
                }
                catch (Exception e) {
                    LOG.info("ImageUtils$Orientation.getOrientation(): Exception while trying to read the orientation data from the EXIF.  Ignoring and just returning null" + e);
                }
            }
            return null;
        }

        /**
         * Tries to read the orientation data from the given image {@link Metadata}.  Returns <code>null</code> if not
         * found or if an error occurs.
         */
        @Nullable
        public static Orientation getOrientation(@Nullable final Metadata metadata) {
            if (metadata != null) {
                final Directory exifIfd0Directory = metadata.getDirectory(ExifIFD0Directory.class);
                if (exifIfd0Directory != null) {
                    try {
                        return findById(exifIfd0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION));
                    }
                    catch (Exception e) {
                        LOG.info("ImageUtils$Orientation.getOrientation(): Exception while trying to read the orientation data from the EXIF.  Ignoring and just returning null");
                    }
                }
            }
            return null;
        }

        private final int id;
        @Nullable
        private final Scalr.Rotation rotation;
        @Nullable
        private final Scalr.Rotation flip;

        private Orientation(final int id, @Nullable final Scalr.Rotation rotation, @Nullable final Scalr.Rotation flip) {
            this.id = id;
            this.rotation = rotation;
            this.flip = flip;
        }

        public int getId() {
            return id;
        }

        @Nullable
        public BufferedImage transform(@Nullable final BufferedImage image) {
            BufferedImage transformedImage = image;
            if (transformedImage != null) {
                if (rotation != null) {
                    transformedImage = Scalr.rotate(transformedImage, rotation);
                }
                if (flip != null) {
                    transformedImage = Scalr.rotate(transformedImage, flip);
                }
            }
            return transformedImage;
        }
    }

    private static final class GeolocationImpl implements Geolocation {
        @Nullable
        private final Double latitude;

        @Nullable
        private final Double longitude;

        @Nullable
        private final Float heading;

        @Nullable
        private final String headingRef;

        @Nullable
        private final Float altitude;

        @Nullable
        private final Integer altitudeRef;

        @Nullable
        private final Float precision;

        @Nullable
        private final String gpsDatestamp;

        @Nullable
        private final String gpsTimestamp;

        private GeolocationImpl(@NotNull final GpsDirectory gpsDirectory) {
            final String latitudeRef = gpsDirectory.getString(GpsDirectory.TAG_GPS_LATITUDE_REF);
            final String longitudeRef = gpsDirectory.getString(GpsDirectory.TAG_GPS_LONGITUDE_REF);
            final Rational[] latitudeRationals = gpsDirectory.getRationalArray(GpsDirectory.TAG_GPS_LATITUDE);
            final Rational[] longitudeRationals = gpsDirectory.getRationalArray(GpsDirectory.TAG_GPS_LONGITUDE);
            latitude = (latitudeRationals == null) ? null : GeoLocation.degreesMinutesSecondsToDecimal(latitudeRationals[0], latitudeRationals[1], latitudeRationals[2], "S".equals(latitudeRef));
            longitude = (longitudeRationals == null) ? null : GeoLocation.degreesMinutesSecondsToDecimal(longitudeRationals[0], longitudeRationals[1], longitudeRationals[2], "W".equals(longitudeRef));

            final Rational headingRational = gpsDirectory.getRational(GpsDirectory.TAG_GPS_IMG_DIRECTION);
            heading = (headingRational == null) ? null : headingRational.floatValue();
            headingRef = gpsDirectory.getString(GpsDirectory.TAG_GPS_IMG_DIRECTION_REF);

            final Rational altitudeRational = gpsDirectory.getRational(GpsDirectory.TAG_GPS_ALTITUDE);
            altitude = (altitudeRational == null) ? null : altitudeRational.floatValue();

            Integer altitudeRefInteger;
            try {
                altitudeRefInteger = gpsDirectory.getInt(GpsDirectory.TAG_GPS_ALTITUDE_REF);
            }
            catch (MetadataException e) {
                altitudeRefInteger = null;
            }
            altitudeRef = altitudeRefInteger;

            final Rational precisionRational = gpsDirectory.getRational(GpsDirectory.TAG_GPS_DOP);
            precision = (precisionRational == null) ? null : precisionRational.floatValue();

            gpsDatestamp = gpsDirectory.getString(GpsDirectory.TAG_GPS_DATE_STAMP);

            final GpsDescriptor gpsDescriptor = new GpsDescriptor(gpsDirectory);
            gpsTimestamp = gpsDescriptor.getGpsTimeStampDescription();
        }

        @Override
        @Nullable
        public Double getLatitude() {
            return latitude;
        }

        @Override
        @Nullable
        public Double getLongitude() {
            return longitude;
        }

        @Override
        @Nullable
        public Float getHeading() {
            return heading;
        }

        @Override
        @Nullable
        public String getHeadingRef() {
            return headingRef;
        }

        @Override
        @Nullable
        public Float getAltitude() {
            return altitude;
        }

        @Override
        @Nullable
        public Integer getAltitudeRef() {
            return altitudeRef;
        }

        @Override
        @Nullable
        public Float getGpsPrecision() {
            return precision;
        }

        @Override
        @Nullable
        public String getGpsDatestamp() {
            return gpsDatestamp;
        }

        @Override
        @Nullable
        public String getGpsTimestamp() {
            return gpsTimestamp;
        }
    }

    /**
     * Tries to read the given <code>imageBytes</code> and returns <code>true</code> if it's a GIF, JPEG, or PNG image;
     * returns <code>false</code> otherwise.  Returns <code>false</code> if the given byte array is <code>null</code> or
     * empty.
     */
    public static boolean isImage(@Nullable final byte[] imageBytes) {
        return createImageIcon(imageBytes) != null;
    }

    /**
     * Tries to read the given <code>imageBytes</code> and interpret as an {@link ImageIcon}.  Returns <code>null</code>
     * if the given byte array is <code>null</code> or empty or if the bytes cannot be read as an image.  Currently only
     * supports GIF, JPEG, and PNG.
     */
    @Nullable
    public static ImageIcon createImageIcon(@Nullable final byte[] imageBytes) {
        if (imageBytes != null && imageBytes.length > 0) {
            final ImageIcon imageIcon = new ImageIcon(imageBytes);

            // width and height will be -1 if it's not actually an image
            if (imageIcon.getIconWidth() >= 0 && imageIcon.getIconHeight() >= 0) {
                return imageIcon;
            }
        }

        return null;
    }

    /**
     * Tries to create a thumbnail of the given image with the given desired dimensions.  Returns <code>null</code> if
     * the given byte array is <code>null</code> or empty, if the desired <code>lengthOfLongestSideInPixels</code> is
     * zero or negative, or if the bytes cannot be read as an image.  This method will attempt to read orientation info
     * from the EXIF and, if necessary, rotate/flip the thumbnail appropriately.
     *
     * @throws IOException if a problem occurs while reading the image or generating the thumbnail
     */
    @Nullable
    public static BufferedImage createThumbnail(@Nullable final byte[] imageBytes, final int lengthOfLongestSideInPixels) throws IOException {
        if (imageBytes != null && imageBytes.length > 0 && lengthOfLongestSideInPixels > 0) {

            Orientation orientation = Orientation.getOrientation(new ByteArrayInputStream(imageBytes));
            if (orientation == null) {
                orientation = Orientation.ORIENTATION_1;
            }

            final BufferedImage image = convertToBufferedImage(imageBytes);
            if (image != null) {
                return orientation.transform(Scalr.resize(image, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, lengthOfLongestSideInPixels));
            }
        }

        return null;
    }

    @Nullable
    public static Geolocation getGeolocation(@Nullable final byte[] imageBytes) throws ImageProcessingException, IOException {
        if (imageBytes != null && imageBytes.length > 0) {

            final Metadata metadata = ImageMetadataReader.readMetadata(new BufferedInputStream(new ByteArrayInputStream(imageBytes)), false);
            if (metadata != null) {
                final GpsDirectory gpsDirectory = metadata.getDirectory(GpsDirectory.class);
                if (gpsDirectory != null) {
                    return new GeolocationImpl(gpsDirectory);
                }
            }
        }
        return null;
    }

    @Nullable
    public static Orientation getOrientation(@Nullable final byte[] imageBytes) {
        if (imageBytes != null && imageBytes.length > 0) {
            return Orientation.getOrientation(new ByteArrayInputStream(imageBytes));
        }
        return null;
    }

    @Nullable
    public static BufferedImage convertToBufferedImage(@Nullable final byte[] imageBytes) throws IOException {
        if (imageBytes != null && imageBytes.length > 0) {
            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        }
        return null;
    }

    @Nullable
    public static byte[] convertToByteArray(@Nullable final BufferedImage image) throws IOException {
        if (image != null) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            if (ImageIO.write(image, "JPG", byteArrayOutputStream)) {
                return byteArrayOutputStream.toByteArray();
            }
        }
        return null;
    }

    private ImageUtils() {
        // private to prevent instantiation
    }
}
