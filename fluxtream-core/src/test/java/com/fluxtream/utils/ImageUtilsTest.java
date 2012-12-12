package com.fluxtream.utils;

import java.awt.Dimension;
import java.io.IOException;
import javax.swing.ImageIcon;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * <p>
 * <code>ImageUtilsTest</code> tests the {@link ImageUtils} class.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class ImageUtilsTest {

    private static final byte[] IMAGE_1 = readFileAsBytes("/test_image1.jpg");
    private static final byte[] IMAGE_2 = readFileAsBytes("/test_image2.jpg");
    private static final byte[] IMAGE_3 = readFileAsBytes("/test_image3.png");
    private static final byte[] IMAGE_4 = readFileAsBytes("/test_image4.gif");
    private static final byte[] IMAGE_5 = readFileAsBytes("/test_image5.tiff");
    private static final byte[] IMAGE_6 = "This is not an image".getBytes();

    private static final Dimension IMAGE_1_EXPECTED_SIZE = new Dimension(265, 284);
    private static final Dimension IMAGE_2_EXPECTED_SIZE = new Dimension(3264, 2448);
    private static final Dimension IMAGE_3_EXPECTED_SIZE = new Dimension(1024, 1365);
    private static final Dimension IMAGE_4_EXPECTED_SIZE = new Dimension(265, 284);

    private static byte[] readFileAsBytes(final String filePath) {
        byte[] imageBytes;
        try {
            imageBytes = IOUtils.toByteArray(HashUtilsTest.class.getResourceAsStream(filePath));
        }
        catch (IOException e) {
            imageBytes = "Failed to read the image file".getBytes();
            System.err.println("IOException while trying to read the file for IMAGE_BYTES:" + e);
        }
        return imageBytes;
    }

    @Test
    public void testIsImage() throws Exception {
        Assert.assertTrue(ImageUtils.isImage(IMAGE_1));
        Assert.assertTrue(ImageUtils.isImage(IMAGE_2));
        Assert.assertTrue(ImageUtils.isImage(IMAGE_3));
        Assert.assertTrue(ImageUtils.isImage(IMAGE_4));
        Assert.assertFalse(ImageUtils.isImage(IMAGE_5));    // TIFFs are not supported
        Assert.assertFalse(ImageUtils.isImage(IMAGE_6));    // not actually an image
    }

    @Test
    public void testCreateImageIcon() throws Exception {

        final ImageIcon imageIcon1 = ImageUtils.createImageIcon(IMAGE_1);
        final ImageIcon imageIcon2 = ImageUtils.createImageIcon(IMAGE_2);
        final ImageIcon imageIcon3 = ImageUtils.createImageIcon(IMAGE_3);
        final ImageIcon imageIcon4 = ImageUtils.createImageIcon(IMAGE_4);
        final ImageIcon imageIcon5 = ImageUtils.createImageIcon(IMAGE_5);
        final ImageIcon imageIcon6 = ImageUtils.createImageIcon(IMAGE_6);

        Assert.assertNotNull(imageIcon1);
        Assert.assertNotNull(imageIcon2);
        Assert.assertNotNull(imageIcon3);
        Assert.assertNotNull(imageIcon4);
        Assert.assertNull(imageIcon5);      // TIFFs are not supported
        Assert.assertNull(imageIcon6);      // not actually an image
        Assert.assertNull(ImageUtils.createImageIcon(null));
        Assert.assertNull(ImageUtils.createImageIcon(new byte[]{}));

        Assert.assertEquals(IMAGE_1_EXPECTED_SIZE, new Dimension(imageIcon1.getIconWidth(), imageIcon1.getIconHeight()));
        Assert.assertEquals(IMAGE_2_EXPECTED_SIZE, new Dimension(imageIcon2.getIconWidth(), imageIcon2.getIconHeight()));
        Assert.assertEquals(IMAGE_3_EXPECTED_SIZE, new Dimension(imageIcon3.getIconWidth(), imageIcon3.getIconHeight()));
        Assert.assertEquals(IMAGE_4_EXPECTED_SIZE, new Dimension(imageIcon4.getIconWidth(), imageIcon4.getIconHeight()));
    }

    @Test
    public void testCreateThumbnail() throws Exception {
        Assert.assertNull(ImageUtils.createThumbnail(null, -1));
        Assert.assertNull(ImageUtils.createThumbnail(null, 0));
        Assert.assertNull(ImageUtils.createThumbnail(null, 100));
        Assert.assertNull(ImageUtils.createThumbnail(new byte[]{}, -1));
        Assert.assertNull(ImageUtils.createThumbnail(new byte[]{}, 0));
        Assert.assertNull(ImageUtils.createThumbnail(new byte[]{}, 100));
        Assert.assertNull(ImageUtils.createThumbnail(IMAGE_1, -1));
        Assert.assertNull(ImageUtils.createThumbnail(IMAGE_1, 0));

        testCreateThumbnailHelper(IMAGE_1, 50, new Dimension(47, 50));
        testCreateThumbnailHelper(IMAGE_1, 100, new Dimension(93, 100));
        testCreateThumbnailHelper(IMAGE_1, (int)Math.max(IMAGE_1_EXPECTED_SIZE.getHeight(), IMAGE_1_EXPECTED_SIZE.getWidth()), IMAGE_1_EXPECTED_SIZE);

        testCreateThumbnailHelper(IMAGE_2, 50, new Dimension(50, 38));
        testCreateThumbnailHelper(IMAGE_2, 100, new Dimension(100, 75));

        testCreateThumbnailHelper(IMAGE_3, 50, new Dimension(38, 50));
        testCreateThumbnailHelper(IMAGE_3, 100, new Dimension(75, 100));

        testCreateThumbnailHelper(IMAGE_4, 50, new Dimension(47, 50));
        testCreateThumbnailHelper(IMAGE_4, 100, new Dimension(93, 100));
    }

    private void testCreateThumbnailHelper(@NotNull final byte[] imageBytes, final int lengthOfLongestSideInPixels, @NotNull final Dimension expectedDimension) throws IOException {
        final byte[] thumbnail1 = ImageUtils.createThumbnail(imageBytes, lengthOfLongestSideInPixels);
        Assert.assertNotNull(thumbnail1);
        final ImageIcon thumbnailIcon1 = ImageUtils.createImageIcon(thumbnail1);
        Assert.assertNotNull(thumbnailIcon1);
        Assert.assertEquals(expectedDimension, new Dimension(thumbnailIcon1.getIconWidth(), thumbnailIcon1.getIconHeight()));
    }
}
