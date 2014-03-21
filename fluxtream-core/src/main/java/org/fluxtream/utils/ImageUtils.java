package org.fluxtream.utils;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.lang.Rational;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.GpsDescriptor;
import com.drew.metadata.exif.GpsDirectory;
import org.fluxtream.domain.Geolocation;
import org.fluxtream.images.Image;
import org.fluxtream.images.ImageOrientation;
import org.fluxtream.images.ImageType;
import org.fluxtream.images.JpegImage;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.fluxtream.aspects.FlxLogger;
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
    private static final FlxLogger LOG = FlxLogger.getLogger(ImageUtils.class);

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
    public static boolean isSupportedImage(@Nullable final byte[] imageBytes) {
        return getImageType(imageBytes) != null;
    }

    /**
     * Tries to create a JPEG thumbnail of the given image with the given desired dimensions.  Returns <code>null</code>
     * if the given byte array is <code>null</code> or empty, if the desired <code>lengthOfLongestSideInPixels</code> is
     * zero or negative, or if the bytes cannot be read as an image.  This method will attempt to read orientation info
     * from the EXIF and, if necessary, rotate/flip the thumbnail appropriately.  Note that if the image has an alpha
     * channel, it will be discarded before generating the thumbnail.
     *
     * @throws IOException if a problem occurs while reading the image or generating the thumbnail
     */
    @Nullable
    public static Image createJpegThumbnail(@Nullable final byte[] imageBytes, final int lengthOfLongestSideInPixels) throws IOException {
        if (imageBytes != null && imageBytes.length > 0 && lengthOfLongestSideInPixels > 0) {

            ImageOrientation orientation = ImageOrientation.getOrientation(new ByteArrayInputStream(imageBytes));
            if (orientation == null) {
                orientation = ImageOrientation.ORIENTATION_1;
            }

            try {
                BufferedImage image = convertToBufferedImage(imageBytes);
                if (image != null) {
                    // drop the alpha channel, if one exists
                    if (image.getColorModel().hasAlpha()) {
                        image = dropAlphaChannel(image);
                    }
                    return JpegImage.create(orientation.transform(Scalr.resize(image, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, lengthOfLongestSideInPixels)));
                }
            }
            catch (Exception e) {
                final String message = "Exception while trying to create a thumbnail";
                LOG.error(message, e);
                throw new IOException(e);
            }
        }

        return null;
    }

    // I stole this from: https://github.com/thebuzzmedia/imgscalr/issues/82#issuecomment-11776976
    @NotNull
    private static BufferedImage dropAlphaChannel(@NotNull final BufferedImage srcImage) {
        final BufferedImage convertedImg = new BufferedImage(srcImage.getWidth(), srcImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        convertedImg.getGraphics().drawImage(srcImage, 0, 0, null);

        return convertedImg;
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
    public static ImageOrientation getOrientation(@Nullable final byte[] imageBytes) {
        if (imageBytes != null && imageBytes.length > 0) {
            return ImageOrientation.getOrientation(new ByteArrayInputStream(imageBytes));
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
    public static byte[] convertToJpegByteArray(@Nullable final BufferedImage image) throws IOException {
        if (image != null) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            if (ImageIO.write(image, "JPG", byteArrayOutputStream)) {
                return byteArrayOutputStream.toByteArray();
            }
        }
        return null;
    }

    /**
     * Returns the {@link ImageType} of the given image, or <code>null</code> if the type is unknown, not supported, or
     * if an error occurs while trying to read the image. Returns <code>null</code> if the given image byte array is
     * <code>null</code> or empty.
     */
    @Nullable
    public static ImageType getImageType(@Nullable final byte[] imageBytes) {
        if ((imageBytes != null) && (imageBytes.length > 0)) {
            try {
                final ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes));
                final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(iis);
                if (imageReaders != null) {
                    while (imageReaders.hasNext()) {
                        final ImageReader reader = imageReaders.next();
                        final ImageType imageType = ImageType.findByFormatName(reader.getFormatName());
                        if (imageType != null) {
                            return imageType;
                        }
                    }
                }
            }
            catch (IOException e) {
                LOG.error("IOException while trying to read the image type, returning null");
            }
        }

        return null;
    }

    private ImageUtils() {
        // private to prevent instantiation
    }
}
