package org.bodytrack.datastore;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * <p>
 * <code>KeyValueStoreHelperTest</code> tests the {@link KeyValueStoreHelper} class.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
@RunWith(JUnit4.class)
public class KeyValueStoreHelperTest {
    @Test
    public void testIsValidKey() {
        Assert.assertFalse(KeyValueStoreHelper.isValidKey(null));
        Assert.assertFalse(KeyValueStoreHelper.isValidKey(""));
        Assert.assertFalse(KeyValueStoreHelper.isValidKey("."));
        Assert.assertFalse(KeyValueStoreHelper.isValidKey(".."));
        Assert.assertFalse(KeyValueStoreHelper.isValidKey(".a."));
        Assert.assertFalse(KeyValueStoreHelper.isValidKey("a."));
        Assert.assertFalse(KeyValueStoreHelper.isValidKey(".a"));
        Assert.assertFalse(KeyValueStoreHelper.isValidKey("$"));
        Assert.assertFalse(KeyValueStoreHelper.isValidKey("="));
        Assert.assertFalse(KeyValueStoreHelper.isValidKey("a.b..c"));

        Assert.assertTrue(KeyValueStoreHelper.isValidKey("a"));
        Assert.assertTrue(KeyValueStoreHelper.isValidKey("a.b"));
        Assert.assertTrue(KeyValueStoreHelper.isValidKey("a.b.c"));
        Assert.assertTrue(KeyValueStoreHelper.isValidKey("a-b-c"));
        Assert.assertTrue(KeyValueStoreHelper.isValidKey("a_b_c"));
        Assert.assertTrue(KeyValueStoreHelper.isValidKey("a._-b"));
        Assert.assertTrue(KeyValueStoreHelper.isValidKey("____"));
        Assert.assertTrue(KeyValueStoreHelper.isValidKey("----"));
    }
}
