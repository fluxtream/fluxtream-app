package org.fluxtream.images;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * <code>ImageType</code> is an enum representing the various types of supported images.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public enum ImageType {
    JPEG("image/jpeg", "jpg", "JPEG"), PNG("image/png", "png", "png"), GIF("image/gif", "gif", "gif");

    @NotNull
    private final String mediaType;
    @NotNull
    private final String fileExtension;
    @NotNull
    private final String imageReaderFormatName;

    private static final Map<String, ImageType> IMAGE_TYPE_BY_MEDIA_TYPE;
    private static final Map<String, ImageType> IMAGE_TYPE_BY_FILE_EXTENSION;
    private static final Map<String, ImageType> IMAGE_TYPE_BY_FORMAT_NAME;

    static {
        final Map<String, ImageType> imageTypeByMediaType = new HashMap<String, ImageType>(ImageType.values().length);
        final Map<String, ImageType> imageTypeByFileExtension = new HashMap<String, ImageType>(ImageType.values().length);
        final Map<String, ImageType> imageTypeByFormatName = new HashMap<String, ImageType>(ImageType.values().length);
        for (final ImageType imageType : ImageType.values()) {
            imageTypeByMediaType.put(imageType.getMediaType(), imageType);
            imageTypeByFileExtension.put(imageType.getFileExtension(), imageType);
            imageTypeByFormatName.put(imageType.getImageReaderFormatName(), imageType);
        }
        IMAGE_TYPE_BY_MEDIA_TYPE = Collections.unmodifiableMap(imageTypeByMediaType);
        IMAGE_TYPE_BY_FILE_EXTENSION = Collections.unmodifiableMap(imageTypeByFileExtension);
        IMAGE_TYPE_BY_FORMAT_NAME = Collections.unmodifiableMap(imageTypeByFormatName);
    }

    @Nullable
    public static ImageType findByMediaType(@Nullable final String mediaType) {
        return IMAGE_TYPE_BY_MEDIA_TYPE.get(mediaType);
    }

    @Nullable
    public static ImageType findByFileExtension(@Nullable final String fileExtension) {
        return IMAGE_TYPE_BY_FILE_EXTENSION.get(fileExtension);
    }

    @Nullable
    public static ImageType findByFormatName(@Nullable final String formatName) {
        return IMAGE_TYPE_BY_FORMAT_NAME.get(formatName);
    }

    ImageType(@NotNull final String mediaType, @NotNull final String fileExtension, @NotNull final String imageReaderFormatName) {
        this.mediaType = mediaType;
        this.fileExtension = fileExtension;
        this.imageReaderFormatName = imageReaderFormatName;
    }

    @NotNull
    public String getMediaType() {
        return mediaType;
    }

    @NotNull
    public String getFileExtension() {
        return fileExtension;
    }

    @NotNull
    public String getImageReaderFormatName() {
        return imageReaderFormatName;
    }
}
