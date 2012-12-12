package com.fluxtream.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.swing.ImageIcon;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * <code>ImageUtils</code> provides helpful methods for dealing with images.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class ImageUtils {

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
     * zero or negative, or if the bytes cannot be read as an image.
     *
     * @throws IOException if a problem occurs while reading the image or generating the thumbnail
     */
    @Nullable
    public static byte[] createThumbnail(@Nullable final byte[] imageBytes, final int lengthOfLongestSideInPixels) throws IOException {
        if (imageBytes != null && imageBytes.length > 0 && lengthOfLongestSideInPixels > 0) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Thumbnails
                    .of(new ByteArrayInputStream(imageBytes))
                    .size(lengthOfLongestSideInPixels, lengthOfLongestSideInPixels)
                    .outputQuality(1.0f)
                    .toOutputStream(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }

        return null;
    }

    private ImageUtils() {
        // private to prevent instantiation
    }
}
