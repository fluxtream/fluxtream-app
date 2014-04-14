package org.fluxtream.core.images;

import java.awt.image.BufferedImage;
import java.io.IOException;
import org.fluxtream.core.utils.ImageUtils;
import org.fluxtream.core.aspects.FlxLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The <code>JpegImage</code> simply takes a {@link BufferedImage} and converts it to a JPEG.
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class JpegImage implements Image {

    private static final FlxLogger LOG = FlxLogger.getLogger(JpegImage.class);

    @NotNull
    private final byte[] imageBytes;
    private final int width;
    private final int height;

    @Nullable
    public static Image create(@Nullable final BufferedImage image) {
        if (image != null) {
            try {
                return new JpegImage(image);
            }
            catch (IOException e) {
                LOG.error("IOException while trying to create", e);
            }
        }
        return null;
    }

    public JpegImage(@NotNull final BufferedImage image) throws IOException {
        width = image.getWidth();
        height = image.getHeight();
        final byte[] tempImageBytes = ImageUtils.convertToJpegByteArray(image);
        if (tempImageBytes == null) {
            throw new IOException("Failed to convert the thumbnail to a JPEG");
        }
        this.imageBytes = tempImageBytes;
    }

    @Override
    @NotNull
    public byte[] getBytes() {
        return imageBytes;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
