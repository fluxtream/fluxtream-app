package org.bodytrack.datastore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * Interface for key-value store.
 * </p>
 * <p>
 *    Keys are string with restricted content:
 *    <ul>
 *       <li>key consists of <code>A-Z</code>, <code>a-z</code>, <code>0-9</code>, <code>-</code> (dash), <code>_</code>
 *       (underscore), and <code>.</code> (dot).</li>
 *       <li>dot indicates hierarchy, like a subdirectory.  Key cannot start or end with dot, nor can two or more dots
 *       occur without non-dot characters in-between.</li>
 *    </ul>
 * </p>
 * <p>
 * Examples of valid keys:  "<code>abc</code>", "<code>abc.def</code>", "<code>abc.def.ghi</code>", "<code>123a_b-c</code>"
 * </p>
 * <p>
 * Examples of invalid keys: "", "<code>.</code>", "<code>.abc</code>", "<code>def.</code>", "<code>abc..def</code>", "<code>abc$</code>"
 * </p>
 * <p>
 * Warning:  keys may or may not be case-sensitive, depending on the underlying implementation of the datastore(!).
 * User of the store should not assume one way or the other.
 * </p>
 * @author Randy Sargent (rsargent@cmu.edu)
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface KeyValueStore<ValueType> {
    /**
     * Checks whether a key exists.  Returns <code>true</code> if found, <code>false</code> otherwise. Always returns
     * <code>false</code> if the <code>key</code> is <code>null</code>.
     */
    boolean hasKey(@Nullable final String key);

    /**
     * Sets the given <code>key</code> to the given <code>value</code>.  The value cannot be <code>null</code>.  Returns
     * <code>true</code> if the value was successfully written, <code>false</code> otherwise.  If the key is invalid
     * and/or the value is empty, this method does nothing other than return <code>false</code>.
     *
     * @throws IllegalArgumentException If <code>key</code> and/or <code>value</code> is null
     */
    boolean set(@NotNull final String key, @NotNull final ValueType value) throws IllegalArgumentException;

    /**
     * Returns the value associated with the given <code>key</code>.  Returns <code>null</code> if the key is not found
     * or if the key is <code>null</code>.
     */
    @Nullable
    ValueType get(@Nullable final String key);

    /**
     * Deletes the given <code>key</code> if present.  Returns <code>true</code> if deleted, <code>false</code> if not
     * present or if deletion fails. Always returns <code>false</code> if the <code>key</code> is <code>null</code>.
     */
    boolean delete(@Nullable final String key);
}