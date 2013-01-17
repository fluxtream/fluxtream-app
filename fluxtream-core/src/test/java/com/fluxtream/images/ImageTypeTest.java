package com.fluxtream.images;

import junit.framework.Assert;
import org.junit.Test;

/**
 * <p>
 * <code>ImageTypeTest</code> tests the {@link ImageType} class.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class ImageTypeTest {
    @Test
    public void testGetMediaType() {
        Assert.assertEquals("image/jpeg", ImageType.JPEG.getMediaType());
        Assert.assertEquals("image/png", ImageType.PNG.getMediaType());
        Assert.assertEquals("image/gif", ImageType.GIF.getMediaType());
    }

    @Test
    public void testGetFileExtension() {
        Assert.assertEquals("jpg", ImageType.JPEG.getFileExtension());
        Assert.assertEquals("png", ImageType.PNG.getFileExtension());
        Assert.assertEquals("gif", ImageType.GIF.getFileExtension());
    }

    @Test
    public void testGetImageReaderFormatName() {
        Assert.assertEquals("JPEG", ImageType.JPEG.getImageReaderFormatName());
        Assert.assertEquals("png", ImageType.PNG.getImageReaderFormatName());
        Assert.assertEquals("gif", ImageType.GIF.getImageReaderFormatName());
    }

    @Test
    public void testFindByFormatName() {
        Assert.assertEquals(ImageType.JPEG, ImageType.findByFormatName("JPEG"));
        Assert.assertEquals(ImageType.PNG, ImageType.findByFormatName("png"));
        Assert.assertEquals(ImageType.GIF, ImageType.findByFormatName("gif"));
    }
}