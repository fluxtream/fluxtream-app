package org.fluxtream.utils;

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
    private static final String EXPECTED_PHOTO_HASH_SHA256 = "1ac240d22be5b76400d1ee8b75b2441566782017002665044f3af781143b7c61";
    private static final String EXPECTED_TEXT_HASH_SHA256 = "045c998e67248615c14ca05b5947a47d218f982341e8b676e2ae17fed4a4615c";
    private static final String EXPECTED_PHOTO_HASH_MD5 = "29230eb48ad70564f556d784a32fb934";
    private static final String EXPECTED_TEXT_HASH_MD5 = "613eb874e96c77053a270a4e5530d5b9";

    static {
        byte[] photoBytes;
        try {
            photoBytes = IOUtils.toByteArray(HashUtilsTest.class.getResourceAsStream("/images/test_image1.jpg"));
        }
        catch (IOException e) {
            photoBytes = "Failed to read the image file".getBytes();
            System.err.println("IOException while trying to read the file for PHOTO_BYTES:" + e);
        }
        PHOTO_BYTES = photoBytes;
    }

    @Test
    public void testComputeSha256Hash() {
        try {
            Assert.assertEquals(EXPECTED_PHOTO_HASH_SHA256, HashUtils.computeSha256Hash(PHOTO_BYTES));
            Assert.assertEquals(EXPECTED_TEXT_HASH_SHA256, HashUtils.computeSha256Hash(TEXT_BYTES));
        }
        catch (NoSuchAlgorithmException e) {
            Assert.fail("Call to FluxtreamCapturePhotoFacet.testComputeSha256Hash() threw a NoSuchAlgorithmException: " + e);
        }
    }

    @Test
    public void testComputeMd5Hash() {
        try {
            Assert.assertEquals(EXPECTED_PHOTO_HASH_MD5, HashUtils.computeMd5Hash(PHOTO_BYTES));
            Assert.assertEquals(EXPECTED_TEXT_HASH_MD5, HashUtils.computeMd5Hash(TEXT_BYTES));
        }
        catch (NoSuchAlgorithmException e) {
            Assert.fail("Call to FluxtreamCapturePhotoFacet.testComputeMd5Hash() threw a NoSuchAlgorithmException: " + e);
        }
    }
}
