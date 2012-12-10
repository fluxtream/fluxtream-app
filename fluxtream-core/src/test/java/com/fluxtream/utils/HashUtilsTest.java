package com.fluxtream.utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>
 * <code>HashUtilsTest</code> tests the {@link HashUtils} class.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class HashUtilsTest {

    private static final byte[] PHOTO_BYTES;
    private static final byte[] TEXT_BYTES = "What I cannot create, I do not understand.\n".getBytes();
    private static final String EXPECTED_PHOTO_HASH = "1ac240d22be5b76400d1ee8b75b2441566782017002665044f3af781143b7c61";
    private static final String EXPECTED_TEXT_HASH = "045c998e67248615c14ca05b5947a47d218f982341e8b676e2ae17fed4a4615c";

    static {
        byte[] photoBytes;
        try {
            photoBytes = IOUtils.toByteArray(HashUtilsTest.class.getResourceAsStream("/test_image1.jpg"));
        }
        catch (IOException e) {
            photoBytes = "Failed to read the image file".getBytes();
            System.err.println("IOException while trying to read the file for PHOTO_BYTES:" + e);
        }
        PHOTO_BYTES = photoBytes;
    }

    @Test
    public void testComputeHash() {
        try {
            Assert.assertEquals(EXPECTED_PHOTO_HASH, HashUtils.computeSha256Hash(PHOTO_BYTES));
            Assert.assertEquals(EXPECTED_TEXT_HASH, HashUtils.computeSha256Hash(TEXT_BYTES));
        }
        catch (NoSuchAlgorithmException e) {
            Assert.fail("Call to FluxtreamCapturePhotoFacet.computeHash() threw a NoSuchAlgorithmException: " + e);
        }
    }
}
