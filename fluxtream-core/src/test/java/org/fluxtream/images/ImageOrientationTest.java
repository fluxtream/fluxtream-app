package org.fluxtream.images;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import com.drew.metadata.Metadata;
import org.fluxtream.utils.HashUtilsTest;
import junit.framework.Assert;
import org.junit.Test;

/**
 * <p>
 * <code>ImageOrientationTest</code> tests the {@link ImageOrientation} class.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class ImageOrientationTest {
    @Test
    public void tesGetId() throws Exception {
        Assert.assertEquals(1, ImageOrientation.ORIENTATION_1.getId());
        Assert.assertEquals(2, ImageOrientation.ORIENTATION_2.getId());
        Assert.assertEquals(3, ImageOrientation.ORIENTATION_3.getId());
        Assert.assertEquals(4, ImageOrientation.ORIENTATION_4.getId());
        Assert.assertEquals(5, ImageOrientation.ORIENTATION_5.getId());
        Assert.assertEquals(6, ImageOrientation.ORIENTATION_6.getId());
        Assert.assertEquals(7, ImageOrientation.ORIENTATION_7.getId());
        Assert.assertEquals(8, ImageOrientation.ORIENTATION_8.getId());
    }

    @Test
    public void testFindOrientationById() throws Exception {
        Assert.assertNull(ImageOrientation.findById(0));
        Assert.assertNull(ImageOrientation.findById(-1));
        Assert.assertNull(ImageOrientation.findById(9));
        Assert.assertEquals(ImageOrientation.ORIENTATION_1, ImageOrientation.findById(1));
        Assert.assertEquals(ImageOrientation.ORIENTATION_2, ImageOrientation.findById(2));
        Assert.assertEquals(ImageOrientation.ORIENTATION_3, ImageOrientation.findById(3));
        Assert.assertEquals(ImageOrientation.ORIENTATION_4, ImageOrientation.findById(4));
        Assert.assertEquals(ImageOrientation.ORIENTATION_5, ImageOrientation.findById(5));
        Assert.assertEquals(ImageOrientation.ORIENTATION_6, ImageOrientation.findById(6));
        Assert.assertEquals(ImageOrientation.ORIENTATION_7, ImageOrientation.findById(7));
        Assert.assertEquals(ImageOrientation.ORIENTATION_8, ImageOrientation.findById(8));
    }

    @Test
    public void testGetOrientation() {
        Assert.assertNull(ImageOrientation.getOrientation((Metadata)null));
        Assert.assertNull(ImageOrientation.getOrientation((InputStream)null));
        Assert.assertNull(ImageOrientation.getOrientation(new ByteArrayInputStream("not an image".getBytes())));
        Assert.assertNull(ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/test_image1.jpg")));
        Assert.assertEquals(ImageOrientation.ORIENTATION_6, ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/test_image2.jpg")));
        Assert.assertNull(ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/test_image3.png")));
        Assert.assertNull(ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/test_image4.gif")));
        Assert.assertEquals(ImageOrientation.ORIENTATION_1, ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/test_image5.tiff")));
        Assert.assertEquals(ImageOrientation.ORIENTATION_1, ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/orientation/orientation_1.jpg")));
        Assert.assertEquals(ImageOrientation.ORIENTATION_2, ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/orientation/orientation_2.jpg")));
        Assert.assertEquals(ImageOrientation.ORIENTATION_3, ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/orientation/orientation_3.jpg")));
        Assert.assertEquals(ImageOrientation.ORIENTATION_4, ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/orientation/orientation_4.jpg")));
        Assert.assertEquals(ImageOrientation.ORIENTATION_5, ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/orientation/orientation_5.jpg")));
        Assert.assertEquals(ImageOrientation.ORIENTATION_6, ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/orientation/orientation_6.jpg")));
        Assert.assertEquals(ImageOrientation.ORIENTATION_7, ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/orientation/orientation_7.jpg")));
        Assert.assertEquals(ImageOrientation.ORIENTATION_8, ImageOrientation.getOrientation(HashUtilsTest.class.getResourceAsStream("/images/orientation/orientation_8.jpg")));
    }
}