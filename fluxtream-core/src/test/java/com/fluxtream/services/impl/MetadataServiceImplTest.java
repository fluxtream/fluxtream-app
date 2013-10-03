package com.fluxtream.services.impl;

import java.util.TreeSet;
import org.joda.time.DateTimeConstants;
import org.junit.Assert;
import org.junit.Test;

/**
 * User: candide
 * Date: 02/10/13
 * Time: 18:07
 */
public class MetadataServiceImplTest {
    @Test
    public void testGetDatesBetween() throws Exception {
        MetadataServiceImpl metadataService = new MetadataServiceImpl();
        long now = System.currentTimeMillis();
        TreeSet<String> datesBetween = metadataService.getDatesBetween(now, now);
        Assert.assertTrue(datesBetween.size()==3);
        long then = now - DateTimeConstants.MILLIS_PER_DAY;
        datesBetween = metadataService.getDatesBetween(then, now);
        Assert.assertTrue(datesBetween.size()==4);
        now += DateTimeConstants.MILLIS_PER_DAY;
        datesBetween = metadataService.getDatesBetween(then, now);
        System.out.println(datesBetween);
        Assert.assertTrue(datesBetween.size()==5);
    }
}
