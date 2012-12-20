package com.fluxtream.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * <code>HashUtils</code> helps compute hashes.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class HashUtils {

    /**
     * Creates a SHA-256 hash for the given <code>bytes</code> and returns it as a {@link String} of hex bytes.
     * Guaranteed to not return <code>null</code>, but might throw a {@link NoSuchAlgorithmException} if the SHA-256
     * algorithm is not available.
     *
     * @throws NoSuchAlgorithmException
     */
    @NotNull
    public static String computeSha256Hash(@NotNull final byte[] bytes) throws NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        // add the bytes to the digest
        messageDigest.update(bytes);

        // compute the digest
        final byte[] digest = messageDigest.digest();

        // convert the digest bytes into hex
        final StringBuilder hash = new StringBuilder();
        for (final byte b : digest) {
            hash.append(byteToHexString(b));
        }

        return hash.toString().toLowerCase();
    }

    /** Converts the given byte to a (zero-padded, if necessary) hex {@link String}. */
    private static String byteToHexString(final byte b) {
        final String s = Integer.toHexString(b & 0xff);

        return (s.length() == 1) ? "0" + s : s;
    }

    private HashUtils() {
        // private to prevent instantiation
    }
}
