package org.fluxtream.connectors.mymee;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * User: candide
 * Date: 20/08/13
 * Time: 15:57
 */
public class MymeeCryptoTest {

    /**
     * This is testing that we get the correct result as specified by Thomas' email of May 28th '13
     */
    @Test
    public void testCrypto() throws Exception {
        Security.addProvider(new MymeeCrypto());
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-224", "MymeeCrypto");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            return;
        }
        byte[] result;

        result = digest.digest("flxtest5i88vzf8orqj".getBytes());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < result.length; i++)
            sb.append(String.format("%02x", result[i]));
        assertTrue("fbfc986da9dc02cb5f6395d926f349b1674727be2fefda8d6044187d".equals(sb.toString()));
    }
}
