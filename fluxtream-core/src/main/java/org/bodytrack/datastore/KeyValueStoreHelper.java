package org.bodytrack.datastore;

import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class KeyValueStoreHelper {
    private static final Pattern VALID_KEY_CHARACTERS_PATTERN = Pattern.compile("[a-zA-Z0-9_\\.\\-]+");

    /**
     * Returns <code>true</code> if the given <code>key</code> is non-<code>null</code>, non-empty, consists of only
     * alphanumeric or dot, dash, or underscore characters, does not start or end with a dot, and does not contain two
     * (or more) adjacent dots.
     */
    public static boolean isValidKey(@Nullable final String key) {
        return key != null &&
               key.length() > 0 &&
               !key.startsWith(".") &&
               !key.endsWith(".") &&
               !key.contains("..") &&
               VALID_KEY_CHARACTERS_PATTERN.matcher(key).matches();
    }

    private KeyValueStoreHelper() {
        // private to prevent instantiation
    }
}
