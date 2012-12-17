package org.bodytrack.datastore;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * <p>
 * <code>FilesystemKeyValueStoreTest</code> tests the {@link FilesystemKeyValueStore} class.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
@RunWith(JUnit4.class)
public class FilesystemKeyValueStoreTest {

    private static final String EMPTY_KEY = "";
    private static final String INVALID_KEY = ".invalid_key";
    private static final String VALUE_1_KEY = "feynman.quote";
    private static final String VALUE_2_KEY = "feynman.photo";

    private static File keystoreRootDirectory;

    private static final byte[] VALUE_1 = "What I cannot create, I do not understand.\n".getBytes();
    private static final byte[] VALUE_2;
    private static final byte[] VALUE_3 = "For a successful technology, reality must take precedence over public relations, for nature cannot be fooled.\n".getBytes();

    static {
        byte[] value2;
        try {
            value2 = IOUtils.toByteArray(FilesystemKeyValueStoreTest.class.getResourceAsStream("/images/test_image1.jpg"));
        }
        catch (IOException e) {
            value2 = "Failed to read the image file".getBytes();
            System.err.println("IOException while trying to read the file for VALUE_2:" + e);
        }
        VALUE_2 = value2;
    }

    @BeforeClass
    public static void performSetup() {
        // get Java's temp directory
        final String tempDirectoryPath = System.getProperty("java.io.tmpdir", null);
        Assert.assertNotNull(tempDirectoryPath);

        // create the root directory used by all the tests
        keystoreRootDirectory = new File(tempDirectoryPath, FilesystemKeyValueStore.class.getCanonicalName() + System.nanoTime());
        keystoreRootDirectory.deleteOnExit();

        // make sure the directory exists
        final boolean mkdirsResult = keystoreRootDirectory.mkdirs();

        Assert.assertTrue("Expected to be able to create directory [" + keystoreRootDirectory + "]", mkdirsResult);
    }

    @Test
    public void testConstructor() {
        // test creating a FilesystemKeyValueStore with a null diretory
        try {
            //noinspection ConstantConditions
            new FilesystemKeyValueStore(null);
            Assert.fail("Creating a FilesystemKeyValueStore with a null root directory should fail with an IllegalArgumentException");
        }
        catch (final IllegalArgumentException e) {
            // nothing to do here because we expect it to fail with an IllegalArgumentException
        }

        // test creating a FilesystemKeyValueStore with a non-existent diretory.
        try {
            new FilesystemKeyValueStore(new File("/this/is/a/bogus/directory"));
            Assert.fail("Creating a FilesystemKeyValueStore with a non-existent directory should fail with an IllegalArgumentException");
        }
        catch (final IllegalArgumentException e) {
            // nothing to do here because we expect it to fail with an IllegalArgumentException
        }

        // test creating a FilesystemKeyValueStore with a valid diretory
        try {
            final FilesystemKeyValueStore store = new FilesystemKeyValueStore(keystoreRootDirectory);
            Assert.assertNotNull(store);
        }
        catch (final Exception e) {
            Assert.fail("Creation of the FilesystemKeyValueStore should not have thrown an exception: " + e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testKeyToFile() {
        try {
            FilesystemKeyValueStore.keyToFile(null, null);
            Assert.fail("Calling FilesystemKeyValueStore.keyToFile with null arguments should fail with an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            // nothing to do here because we expect it to fail with an IllegalArgumentException
        }

        try {
            FilesystemKeyValueStore.keyToFile(keystoreRootDirectory, null);
            Assert.fail("Calling FilesystemKeyValueStore.keyToFile with null arguments should fail with an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            // nothing to do here because we expect it to fail with an IllegalArgumentException
        }

        try {
            FilesystemKeyValueStore.keyToFile(null, "the.key");
            Assert.fail("Calling FilesystemKeyValueStore.keyToFile with null arguments should fail with an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            // nothing to do here because we expect it to fail with an IllegalArgumentException
        }

        Assert.assertNull(FilesystemKeyValueStore.keyToFile(keystoreRootDirectory, EMPTY_KEY));
        Assert.assertNull(FilesystemKeyValueStore.keyToFile(keystoreRootDirectory, INVALID_KEY));

        final File valueFile1 = FilesystemKeyValueStore.keyToFile(keystoreRootDirectory, VALUE_1_KEY);
        Assert.assertNotNull(valueFile1);
        Assert.assertEquals(valueFile1.getAbsolutePath(), new File(keystoreRootDirectory, "feynman/quote.val").getAbsolutePath());
        final File valueFile2 = FilesystemKeyValueStore.keyToFile(keystoreRootDirectory, VALUE_2_KEY);
        Assert.assertNotNull(valueFile2);
        Assert.assertEquals(valueFile2.getAbsolutePath(), new File(keystoreRootDirectory, "feynman/photo.val").getAbsolutePath());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testCRUD() {
        final FilesystemKeyValueStore store = new FilesystemKeyValueStore(keystoreRootDirectory);

        // test hasKey -- these should all be false since there's nothing in the keystore yet
        Assert.assertFalse(store.hasKey(null));
        Assert.assertFalse(store.hasKey(EMPTY_KEY));
        Assert.assertFalse(store.hasKey(INVALID_KEY));
        Assert.assertFalse(store.hasKey(VALUE_1_KEY));
        Assert.assertFalse(store.hasKey(VALUE_2_KEY));

        // test get -- these should all be null since there's nothing in the keystore yet
        Assert.assertNull(store.get(null));
        Assert.assertNull(store.get(EMPTY_KEY));
        Assert.assertNull(store.get(INVALID_KEY));
        Assert.assertNull(store.get(VALUE_1_KEY));
        Assert.assertNull(store.get(VALUE_2_KEY));

        // test set

        try {
            Assert.assertFalse(store.set(null, null));
            Assert.fail(".set should have failed when given null arguments");
        }
        catch (IllegalArgumentException e) {
            // nothing to do since we expect it to fail with an IllegalArgumentException
        }

        try {
            Assert.assertFalse(store.set(null, new byte[]{}));
            Assert.fail(".set should have failed when given null arguments");
        }
        catch (IllegalArgumentException e) {
            // nothing to do since we expect it to fail with an IllegalArgumentException
        }

        try {
            Assert.assertFalse(store.set(null, VALUE_1));
            Assert.fail(".set should have failed when given null arguments");
        }
        catch (IllegalArgumentException e) {
            // nothing to do since we expect it to fail with an IllegalArgumentException
        }

        try {
            Assert.assertFalse(store.set(EMPTY_KEY, null));
            Assert.fail(".set should have failed when given null arguments");
        }
        catch (IllegalArgumentException e) {
            // nothing to do since we expect it to fail with an IllegalArgumentException
        }

        Assert.assertFalse(store.set(EMPTY_KEY, new byte[]{}));
        Assert.assertFalse(store.set(EMPTY_KEY, VALUE_1));
        Assert.assertFalse(store.set(INVALID_KEY, VALUE_1));

        Assert.assertTrue(store.set(VALUE_1_KEY, VALUE_1));
        Assert.assertTrue(store.set(VALUE_2_KEY, VALUE_2));

        // now that we've set some values, test hasKey and get again
        Assert.assertFalse(store.hasKey(null));
        Assert.assertFalse(store.hasKey(EMPTY_KEY));
        Assert.assertFalse(store.hasKey(INVALID_KEY));
        Assert.assertTrue(store.hasKey(VALUE_1_KEY));
        Assert.assertTrue(store.hasKey(VALUE_2_KEY));

        Assert.assertNull(store.get(null));
        Assert.assertNull(store.get(EMPTY_KEY));
        Assert.assertNull(store.get(INVALID_KEY));

        final byte[] value1 = store.get(VALUE_1_KEY);
        Assert.assertNotNull(value1);
        Assert.assertTrue(Arrays.equals(VALUE_1, value1));

        final byte[] value2 = store.get(VALUE_2_KEY);
        Assert.assertNotNull(value2);
        Assert.assertTrue(Arrays.equals(VALUE_2, value2));

        // now test overwrite
        Assert.assertTrue(store.set(VALUE_1_KEY, VALUE_3));
        Assert.assertTrue(store.hasKey(VALUE_1_KEY));
        final byte[] value3 = store.get(VALUE_1_KEY);
        Assert.assertNotNull(value3);
        Assert.assertTrue(Arrays.equals(VALUE_3, value3));

        // now test delete
        Assert.assertFalse(store.delete(null));
        Assert.assertFalse(store.delete(EMPTY_KEY));
        Assert.assertFalse(store.delete(INVALID_KEY));
        Assert.assertTrue(store.delete(VALUE_1_KEY));
        Assert.assertTrue(store.delete(VALUE_2_KEY));

        // finally, test hasKey and get again to make sure they return proper
        // values now that the values have been deleted

        // test hasKey -- these should all be false since there's nothing in the keystore anymore
        Assert.assertFalse(store.hasKey(null));
        Assert.assertFalse(store.hasKey(EMPTY_KEY));
        Assert.assertFalse(store.hasKey(INVALID_KEY));
        Assert.assertFalse(store.hasKey(VALUE_1_KEY));
        Assert.assertFalse(store.hasKey(VALUE_2_KEY));

        // test get -- these should all be null since there's nothing in the keystore anymore
        Assert.assertNull(store.get(null));
        Assert.assertNull(store.get(EMPTY_KEY));
        Assert.assertNull(store.get(INVALID_KEY));
        Assert.assertNull(store.get(VALUE_1_KEY));
        Assert.assertNull(store.get(VALUE_2_KEY));
    }
}
