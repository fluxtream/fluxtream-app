package org.fluxtream.core.images;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.fluxtream.core.aspects.FlxLogger;
import org.imgscalr.Scalr;
import org.jetbrains.annotations.Nullable;

/**
 * An enum for recording image orientation and what operation(s) needs to be applied in order to transform it.
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
public enum ImageOrientation {

    ORIENTATION_1(1, null, null),
    ORIENTATION_2(2, null, Scalr.Rotation.FLIP_HORZ),
    ORIENTATION_3(3, Scalr.Rotation.CW_180, null),
    ORIENTATION_4(4, null, Scalr.Rotation.FLIP_VERT),
    ORIENTATION_5(5, Scalr.Rotation.CW_90, Scalr.Rotation.FLIP_HORZ),
    ORIENTATION_6(6, Scalr.Rotation.CW_90, null),
    ORIENTATION_7(7, Scalr.Rotation.CW_270, Scalr.Rotation.FLIP_HORZ),
    ORIENTATION_8(8, Scalr.Rotation.CW_270, null);

    private static final FlxLogger LOG = FlxLogger.getLogger(ImageOrientation.class);

    private static final Map<Integer, ImageOrientation> ID_TO_BAUD_RATE_MAP;

    static {
        final Map<Integer, ImageOrientation> idToOrienationMap = new HashMap<Integer, ImageOrientation>();
        for (final ImageOrientation orientation : ImageOrientation.values()) {
            idToOrienationMap.put(orientation.getId(), orientation);
        }
        ID_TO_BAUD_RATE_MAP = Collections.unmodifiableMap(idToOrienationMap);
    }

    @Nullable
    public static ImageOrientation findById(final int id) {
        return ID_TO_BAUD_RATE_MAP.get(id);
    }

    /**
     * Tries to read the EXIF orientation data in the image in the {@link InputStream}.  Returns <code>null</code>
     * if not found or if an error occurs.
     */
    @Nullable
    public static ImageOrientation getOrientation(@Nullable final InputStream inputStream) {
        if (inputStream != null) {
            try {
                return getOrientation(ImageMetadataReader.readMetadata(new BufferedInputStream(inputStream), true));
            }
            catch (Exception e) {
                LOG.info("ImageOrientation.getOrientation(): Exception while trying to read the orientation data from the EXIF.  Ignoring and just returning null" + e);
            }
        }
        return null;
    }

    /**
     * Tries to read the orientation data from the given image {@link Metadata}.  Returns <code>null</code> if not
     * found or if an error occurs.
     */
    @Nullable
    public static ImageOrientation getOrientation(@Nullable final Metadata metadata) {
        if (metadata != null) {
            final Directory exifIfd0Directory = metadata.getDirectory(ExifIFD0Directory.class);
            if (exifIfd0Directory != null) {
                try {
                    return findById(exifIfd0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION));
                }
                catch (Exception e) {
                    LOG.info("ImageOrientation.getOrientation(): Exception while trying to read the orientation data from the EXIF.  Ignoring and just returning null");
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

    private ImageOrientation(final int id, @Nullable final Scalr.Rotation rotation, @Nullable final Scalr.Rotation flip) {
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
