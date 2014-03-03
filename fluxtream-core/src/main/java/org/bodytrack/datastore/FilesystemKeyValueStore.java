package org.bodytrack.datastore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.fluxtream.aspects.FlxLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * <code>FilesystemKeyValueStore</code> is a {@link KeyValueStore} based on the filesystem.
 * </p>
 * <p>
 * Each key corresponds to a file in the filesystem, and thus one value per file.  A key names is translated to a file
 * path by converting all "<code>.</code>" characters to "<code>/</code>".
 * </p>
 *
 * @author Randy Sargent (rsargent@cmu.edu)
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class FilesystemKeyValueStore implements KeyValueStore<byte[]> {

    private static final FlxLogger LOG = FlxLogger.getLogger(FilesystemKeyValueStore.class);

    private static final String VALUE_FILE_EXTENSION = ".val";

    /**
     * Converts the given <code>key</code> to a file, if the key is {@link KeyValueStoreHelper#isValidKey(String) valid}.
     * Returns <code>null</code> if the key is invalid.
     *
     * @throws IllegalArgumentException If <code>rootDirectory</code> and/or <code>key</code> is null
     */
    @Nullable
    public static File keyToFile(@NotNull final File rootDirectory, @NotNull final String key) throws IllegalArgumentException {
        //noinspection ConstantConditions
        if (rootDirectory == null || key == null) {
            throw new IllegalArgumentException("The rootDirectory and key must both be non-null.");
        }

        if (KeyValueStoreHelper.isValidKey(key)) {
            // convert dots to slashes and append to the root directory to create a new File
            final String pathKey = key.replace(".", File.separator);
            return new File(rootDirectory, pathKey + VALUE_FILE_EXTENSION);
        }
        return null;
    }

    private final File rootDirectory;

    /**
     * Creates a new <code>FilesystemKeyValueStore</code> with the given <code>rootDirectory</code>.  Throws an
     * {@link IllegalArgumentException} if the given directory is <code>null</code> or does not exist.
     *
     * @throws IllegalArgumentException If <code>rootDirectory</code> is null
     */
    public FilesystemKeyValueStore(@NotNull final File rootDirectory) throws IllegalArgumentException {
        //noinspection ConstantConditions
        if (rootDirectory == null || !rootDirectory.exists()) {
            throw new IllegalArgumentException("The rootDirectory cannot be null and must exist [" + rootDirectory + "].");
        }

        this.rootDirectory = rootDirectory;
    }

    @Override
    public boolean hasKey(@Nullable final String key) {
        return getValueFile(key) != null;
    }

    @Override
    public boolean set(@NotNull final String key, @NotNull final byte[] value) throws IllegalArgumentException {

        //noinspection ConstantConditions
        if (key == null || value == null) {
            throw new IllegalArgumentException("The key and value must both be non-null.");
        }

        if (value.length > 0) {
            final File valueFile = keyToFile(rootDirectory, key);
            if (valueFile != null) {
                try {
                    // make sure the path exists
                    // noinspection ResultOfMethodCallIgnored
                    valueFile.getParentFile().mkdirs();
                    if (valueFile.getParentFile().exists()) {
                        IOUtils.write(value, new FileOutputStream(valueFile));
                        return true;
                    }
                    else {
                        LOG.error("FilesystemKeyValueStore.set(): Failed to create the directories for value file [" + valueFile + "]");
                    }
                }
                catch (IOException e) {
                    LOG.error("IOException while trying to write to file [" + valueFile + "]", e);
                }
            }
        }
        return false;
    }

    @Override
    @Nullable
    public byte[] get(@Nullable final String key) {
        final File valueFile = getValueFile(key);
        if (valueFile != null) {
            try {
                return IOUtils.toByteArray(new FileInputStream(valueFile));
            }
            catch (IOException e) {
                LOG.error("IOException while trying to read file [" + valueFile + "]", e);
            }
        }
        return null;
    }

    @Override
    public boolean delete(@Nullable final String key) {
        final File valueFile = getValueFile(key);
        return valueFile != null && valueFile.delete();
    }

    /**
     * Returns the {@link File} for the given <code>key</code> if and only if the key is non-<code>null</code>, valid
     * AND denotes an existing key.  Returns <code>null</code> otherwise.
     */
    @Nullable
    private File getValueFile(@Nullable final String key) {
        if (key != null) {
            final File valueFile = keyToFile(rootDirectory, key);

            // isFile also tests existence for us
            if (valueFile != null && valueFile.isFile()) {
                return valueFile;
            }
        }

        return null;
    }
}
