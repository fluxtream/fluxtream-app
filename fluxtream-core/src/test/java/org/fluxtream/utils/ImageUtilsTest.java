package org.fluxtream.utils;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import com.drew.imaging.ImageProcessingException;
import org.fluxtream.domain.Geolocation;
import org.fluxtream.images.Image;
import org.fluxtream.images.ImageType;
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

    private static final byte[] IMAGE_1 = readFileAsBytes("/images/test_image1.jpg");
    private static final byte[] IMAGE_2 = readFileAsBytes("/images/test_image2.jpg");
    private static final byte[] IMAGE_3 = readFileAsBytes("/images/test_image3.png");
    private static final byte[] IMAGE_4 = readFileAsBytes("/images/test_image4.gif");
    private static final byte[] IMAGE_5 = readFileAsBytes("/images/test_image5.tiff");
    private static final byte[] IMAGE_6 = readFileAsBytes("/images/test_image6.jpg");
    private static final byte[] NOT_AN_IMAGE = "This is not an image".getBytes();

    private static final Dimension IMAGE_1_EXPECTED_SIZE = new Dimension(265, 284);
    private static final Dimension IMAGE_2_EXPECTED_SIZE = new Dimension(2448, 3264);
    private static final Dimension IMAGE_3_EXPECTED_SIZE = new Dimension(1024, 1365);
    private static final Dimension IMAGE_4_EXPECTED_SIZE = new Dimension(265, 284);
    private static final Dimension IMAGE_6_EXPECTED_SIZE = new Dimension(776, 909);

    private static byte[] readFileAsBytes(final String filePath) {
        byte[] imageBytes;
        try {
            imageBytes = IOUtils.toByteArray(HashUtilsTest.class.getResourceAsStream(filePath));
        }
        catch (IOException e) {
            imageBytes = "Failed to read the image file".getBytes();
            Assert.fail("IOException while trying to read the file for IMAGE_BYTES:" + e);
        }
        return imageBytes;
    }

    @Test
    public void testIsSupportedImage() {
        Assert.assertTrue(ImageUtils.isSupportedImage(IMAGE_1));
        Assert.assertTrue(ImageUtils.isSupportedImage(IMAGE_2));
        Assert.assertTrue(ImageUtils.isSupportedImage(IMAGE_3));
        Assert.assertTrue(ImageUtils.isSupportedImage(IMAGE_4));
        Assert.assertFalse(ImageUtils.isSupportedImage(IMAGE_5));        // TIFFs are not supported
        Assert.assertTrue(ImageUtils.isSupportedImage(IMAGE_6));
        Assert.assertFalse(ImageUtils.isSupportedImage(NOT_AN_IMAGE));   // not actually an image
    }

    @Test
    public void testCreateThumbnail() throws Exception {
        Assert.assertNull(ImageUtils.createJpegThumbnail(null, -1));
        Assert.assertNull(ImageUtils.createJpegThumbnail(null, 0));
        Assert.assertNull(ImageUtils.createJpegThumbnail(null, 100));
        Assert.assertNull(ImageUtils.createJpegThumbnail(new byte[]{}, -1));
        Assert.assertNull(ImageUtils.createJpegThumbnail(new byte[]{}, 0));
        Assert.assertNull(ImageUtils.createJpegThumbnail(new byte[]{}, 100));
        Assert.assertNull(ImageUtils.createJpegThumbnail(IMAGE_1, -1));
        Assert.assertNull(ImageUtils.createJpegThumbnail(IMAGE_1, 0));
        Assert.assertNull(ImageUtils.createJpegThumbnail(NOT_AN_IMAGE, 100));

        testCreateThumbnailHelper(IMAGE_1, 50, new Dimension(47, 50));
        testCreateThumbnailHelper(IMAGE_1, 100, new Dimension(93, 100));
        testCreateThumbnailHelper(IMAGE_1, (int)Math.max(IMAGE_1_EXPECTED_SIZE.getHeight(), IMAGE_1_EXPECTED_SIZE.getWidth()), IMAGE_1_EXPECTED_SIZE);

        testCreateThumbnailHelper(IMAGE_2, 50, new Dimension(38, 50));
        testCreateThumbnailHelper(IMAGE_2, 100, new Dimension(75, 100));
        testCreateThumbnailHelper(IMAGE_2, (int)Math.max(IMAGE_2_EXPECTED_SIZE.getHeight(), IMAGE_2_EXPECTED_SIZE.getWidth()), IMAGE_2_EXPECTED_SIZE);

        testCreateThumbnailHelper(IMAGE_3, 50, new Dimension(38, 50));
        testCreateThumbnailHelper(IMAGE_3, 100, new Dimension(75, 100));
        testCreateThumbnailHelper(IMAGE_3, (int)Math.max(IMAGE_3_EXPECTED_SIZE.getHeight(), IMAGE_3_EXPECTED_SIZE.getWidth()), IMAGE_3_EXPECTED_SIZE);

        testCreateThumbnailHelper(IMAGE_4, 50, new Dimension(47, 50));
        testCreateThumbnailHelper(IMAGE_4, 100, new Dimension(93, 100));
        testCreateThumbnailHelper(IMAGE_4, (int)Math.max(IMAGE_4_EXPECTED_SIZE.getHeight(), IMAGE_4_EXPECTED_SIZE.getWidth()), IMAGE_4_EXPECTED_SIZE);

        testCreateThumbnailHelper(IMAGE_6, 50, new Dimension(43, 50));
        testCreateThumbnailHelper(IMAGE_6, 100, new Dimension(85, 100));
        testCreateThumbnailHelper(IMAGE_6, (int)Math.max(IMAGE_6_EXPECTED_SIZE.getHeight(), IMAGE_6_EXPECTED_SIZE.getWidth()), IMAGE_6_EXPECTED_SIZE);
    }

    private void testCreateThumbnailHelper(@NotNull final byte[] imageBytes, final int lengthOfLongestSideInPixels, @NotNull final Dimension expectedDimension) throws IOException {
        final Image thumbnail = ImageUtils.createJpegThumbnail(imageBytes, lengthOfLongestSideInPixels);
        Assert.assertNotNull(thumbnail);
        Assert.assertEquals(expectedDimension, new Dimension(thumbnail.getWidth(), thumbnail.getHeight()));
    }

    @Test
    public void testConversion() throws Exception {
        Assert.assertNull(ImageUtils.convertToBufferedImage(null));
        Assert.assertNull(ImageUtils.convertToBufferedImage(new byte[]{}));

        final BufferedImage image1 = ImageUtils.convertToBufferedImage(IMAGE_1);
        final BufferedImage image2 = ImageUtils.convertToBufferedImage(IMAGE_2);
        final BufferedImage image3 = ImageUtils.convertToBufferedImage(IMAGE_3);
        final BufferedImage image4 = ImageUtils.convertToBufferedImage(IMAGE_4);
        final BufferedImage image5 = ImageUtils.convertToBufferedImage(IMAGE_5);
        final BufferedImage image6 = ImageUtils.convertToBufferedImage(IMAGE_6);
        final BufferedImage image7 = ImageUtils.convertToBufferedImage(NOT_AN_IMAGE);
        Assert.assertNotNull(image1);
        Assert.assertNotNull(image2);
        Assert.assertNotNull(image3);
        Assert.assertNotNull(image4);
        // TIFFs apparently may or may not be supported, depending on platform, so no null assert here.
        Assert.assertNotNull(image6);
        Assert.assertNull(image7);

        Assert.assertNull(ImageUtils.convertToJpegByteArray(null));
        Assert.assertNotNull(ImageUtils.convertToJpegByteArray(image1));
        Assert.assertNotNull(ImageUtils.convertToJpegByteArray(image2));
        Assert.assertNotNull(ImageUtils.convertToJpegByteArray(image3));
        Assert.assertNotNull(ImageUtils.convertToJpegByteArray(image4));
        Assert.assertNotNull(ImageUtils.convertToJpegByteArray(image6));
        if (image5 != null) {
            Assert.assertNotNull(ImageUtils.convertToJpegByteArray(image5));
        }
    }

    @Test
    public void testExtractLatLong() throws Exception {
        final double delta = 0.00001;

        Assert.assertNull(ImageUtils.getGeolocation(IMAGE_1));

        final Geolocation geolocation1 = ImageUtils.getGeolocation(IMAGE_2);
        Assert.assertNotNull(geolocation1);
        Assert.assertEquals(40.44283333333333, geolocation1.getLatitude(), delta);
        Assert.assertEquals(-79.94633333333333, geolocation1.getLongitude(), delta);
        Assert.assertEquals(318.99533, geolocation1.getHeading(), delta);
        Assert.assertEquals("T", geolocation1.getHeadingRef());
        Assert.assertEquals(318.00183, geolocation1.getAltitude(), delta);
        Assert.assertEquals((Integer)0, geolocation1.getAltitudeRef());
        Assert.assertEquals(null, geolocation1.getGpsPrecision());
        Assert.assertEquals(null, geolocation1.getGpsDatestamp());
        Assert.assertEquals("20:32:31 UTC", geolocation1.getGpsTimestamp());

        try {
            ImageUtils.getGeolocation(IMAGE_3);
            Assert.fail("ImageUtils.getGeolocation() should fail for IMAGE_3 since the image type is not supported.");
        }
        catch (ImageProcessingException e) {
            // nothing to do
        }
        catch (IOException e) {
            Assert.fail("Should not get an IOException for calling ImageUtils.getGeolocation() on IMAGE_3.");
        }

        try {
            ImageUtils.getGeolocation(IMAGE_4);
            Assert.fail("ImageUtils.getGeolocation() should fail for IMAGE_4 since the image type is not supported.");
        }
        catch (ImageProcessingException e) {
            // nothing to do
        }
        catch (IOException e) {
            Assert.fail("Should not get an IOException for calling ImageUtils.getGeolocation() on IMAGE_4.");
        }

        try {
            ImageUtils.getGeolocation(IMAGE_4);
            Assert.fail("ImageUtils.getGeolocation() should fail for IMAGE_4 since the image type is not supported.");
        }
        catch (ImageProcessingException e) {
            // nothing to do
        }
        catch (IOException e) {
            Assert.fail("Should not get an IOException for calling ImageUtils.getGeolocation() on IMAGE_4.");
        }

        Assert.assertNull(ImageUtils.getGeolocation(IMAGE_5));

        final Geolocation geolocation2 = ImageUtils.getGeolocation(IMAGE_6);
        Assert.assertNotNull(geolocation2);
        Assert.assertEquals(45.50066666666667, geolocation2.getLatitude(), delta);
        Assert.assertEquals(9.110333333333333, geolocation2.getLongitude(), delta);
        Assert.assertEquals(null, geolocation2.getHeading());
        Assert.assertEquals(null, geolocation2.getHeadingRef());
        Assert.assertEquals(217, geolocation2.getAltitude(), delta);
        Assert.assertEquals((Integer)0, geolocation2.getAltitudeRef());
        Assert.assertEquals(null, geolocation2.getGpsPrecision());
        Assert.assertEquals("2011:05:06", geolocation2.getGpsDatestamp());
        Assert.assertEquals("7:59:48 UTC", geolocation2.getGpsTimestamp());

        try {
            ImageUtils.getGeolocation(NOT_AN_IMAGE);
            Assert.fail("ImageUtils.getGeolocation() should fail for NOT_AN_IMAGE since the image type is not supported.");
        }
        catch (ImageProcessingException e) {
            // nothing to do
        }
        catch (IOException e) {
            Assert.fail("Should not get an IOException for calling ImageUtils.getGeolocation() on NOT_AN_IMAGE.");
        }

        Assert.assertNull(ImageUtils.getGeolocation(null));
        Assert.assertNull(ImageUtils.getGeolocation(new byte[]{}));
    }

    @Test
    public void testGetImageType() throws IOException {
        Assert.assertEquals(ImageType.JPEG, ImageUtils.getImageType(IMAGE_1));
        Assert.assertEquals(ImageType.JPEG, ImageUtils.getImageType(IMAGE_2));
        Assert.assertEquals(ImageType.PNG, ImageUtils.getImageType(IMAGE_3));
        Assert.assertEquals(ImageType.GIF, ImageUtils.getImageType(IMAGE_4));
        Assert.assertNull(ImageUtils.getImageType(IMAGE_5));
        Assert.assertEquals(ImageType.JPEG, ImageUtils.getImageType(IMAGE_6));
        Assert.assertNull(ImageUtils.getImageType(NOT_AN_IMAGE));
        Assert.assertNull(ImageUtils.getImageType((byte[])null));
        Assert.assertNull(ImageUtils.getImageType(new byte[]{}));
    }
}
