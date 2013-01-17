package com.fluxtream.images;

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

    private static final Map<String, ImageType> IMAGE_TYPE_BY_FORMAT_NAME;

    static {
        final Map<String, ImageType> imageTypeByFormatName = new HashMap<String, ImageType>(ImageType.values().length);
        for (final ImageType imageType : ImageType.values()) {
            imageTypeByFormatName.put(imageType.getImageReaderFormatName(), imageType);
        }
        IMAGE_TYPE_BY_FORMAT_NAME = Collections.unmodifiableMap(imageTypeByFormatName);
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
