package org.fluxtream.core.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;

/**
 * <p>
 * <code>TagFilterTest</code> tests the {@link TagFilter} class.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class TagFilterTest {
    @Test
    public void testCreateMethod() {
        final List<String> emptyList = new ArrayList<String>();
        final List<String> oneTag = Arrays.asList("foo");
        final List<String> multipleTags = Arrays.asList("foo", "bar");
        final List<String> tagsWithOneEmptyItem = Arrays.asList("foo", "");
        final List<String> tagsWithDuplicateItems = Arrays.asList("foo", "Foo", "bar", "  bar ");
        final List<String> tagsWithAllEmptyItems = Arrays.asList("\t", " ", "");

        Assert.assertNull(TagFilter.create(null, null));
        Assert.assertNull(TagFilter.create(null, TagFilter.FilteringStrategy.ALL));
        Assert.assertNull(TagFilter.create(null, TagFilter.FilteringStrategy.ANY));
        Assert.assertNull(TagFilter.create(null, TagFilter.FilteringStrategy.NONE));

        Assert.assertNull(TagFilter.create(emptyList, null));
        Assert.assertNull(TagFilter.create(emptyList, TagFilter.FilteringStrategy.ALL));
        Assert.assertNull(TagFilter.create(emptyList, TagFilter.FilteringStrategy.ANY));
        Assert.assertNull(TagFilter.create(emptyList, TagFilter.FilteringStrategy.NONE));
        Assert.assertNull(TagFilter.create(tagsWithAllEmptyItems, TagFilter.FilteringStrategy.ALL));
        Assert.assertNull(TagFilter.create(tagsWithAllEmptyItems, TagFilter.FilteringStrategy.ANY));
        Assert.assertNull(TagFilter.create(tagsWithAllEmptyItems, TagFilter.FilteringStrategy.NONE));

        testUntagged(TagFilter.create(null, TagFilter.FilteringStrategy.UNTAGGED));
        testUntagged(TagFilter.create(oneTag, TagFilter.FilteringStrategy.UNTAGGED));
        testUntagged(TagFilter.create(multipleTags, TagFilter.FilteringStrategy.UNTAGGED));
        testUntagged(TagFilter.create(tagsWithOneEmptyItem, TagFilter.FilteringStrategy.UNTAGGED));
        testUntagged(TagFilter.create(tagsWithDuplicateItems, TagFilter.FilteringStrategy.UNTAGGED));
        testUntagged(TagFilter.create(tagsWithAllEmptyItems, TagFilter.FilteringStrategy.UNTAGGED));

        testTagFilter(TagFilter.create(oneTag, TagFilter.FilteringStrategy.ALL), TagFilter.FilteringStrategy.ALL, 1, new String[]{"facet.tags like '%,foo,%'"});
        testTagFilter(TagFilter.create(multipleTags, TagFilter.FilteringStrategy.ALL), TagFilter.FilteringStrategy.ALL, 2, new String[]{"facet.tags like '%,foo,%'", "AND", "facet.tags like '%,bar,%'"});
        testTagFilter(TagFilter.create(tagsWithDuplicateItems, TagFilter.FilteringStrategy.ALL), TagFilter.FilteringStrategy.ALL, 2, new String[]{"facet.tags like '%,foo,%'", "AND", "facet.tags like '%,bar,%'"});
        testTagFilter(TagFilter.create(tagsWithOneEmptyItem, TagFilter.FilteringStrategy.ALL), TagFilter.FilteringStrategy.ALL, 1, new String[]{"facet.tags like '%,foo,%'"});

        testTagFilter(TagFilter.create(oneTag, TagFilter.FilteringStrategy.ANY), TagFilter.FilteringStrategy.ANY, 1, new String[]{"facet.tags like '%,foo,%'"});
        testTagFilter(TagFilter.create(multipleTags, TagFilter.FilteringStrategy.ANY), TagFilter.FilteringStrategy.ANY, 2, new String[]{"facet.tags like '%,foo,%'", "OR", "facet.tags like '%,bar,%'"});
        testTagFilter(TagFilter.create(tagsWithDuplicateItems, TagFilter.FilteringStrategy.ANY), TagFilter.FilteringStrategy.ANY, 2, new String[]{"facet.tags like '%,foo,%'", "OR", "facet.tags like '%,bar,%'"});
        testTagFilter(TagFilter.create(tagsWithOneEmptyItem, TagFilter.FilteringStrategy.ANY), TagFilter.FilteringStrategy.ANY, 1, new String[]{"facet.tags like '%,foo,%'"});

        testTagFilter(TagFilter.create(oneTag, TagFilter.FilteringStrategy.NONE), TagFilter.FilteringStrategy.NONE, 1, new String[]{"facet.tags is NULL OR (facet.tags NOT like '%,foo,%')"});
        testTagFilter(TagFilter.create(multipleTags, TagFilter.FilteringStrategy.NONE), TagFilter.FilteringStrategy.NONE, 2, new String[]{"facet.tags is NULL OR (", "facet.tags NOT like '%,foo,%'", "AND", "facet.tags NOT like '%,bar,%'",")"});
        testTagFilter(TagFilter.create(tagsWithDuplicateItems, TagFilter.FilteringStrategy.NONE), TagFilter.FilteringStrategy.NONE, 2, new String[]{"facet.tags is NULL OR (", "facet.tags NOT like '%,foo,%'", "AND", "facet.tags NOT like '%,bar,%'", ")"});
        testTagFilter(TagFilter.create(tagsWithOneEmptyItem, TagFilter.FilteringStrategy.NONE), TagFilter.FilteringStrategy.NONE, 1, new String[]{"facet.tags is NULL OR (facet.tags NOT like '%,foo,%')"});
    }

    private void testUntagged(final TagFilter tagFilter) {
        testTagFilter(tagFilter, TagFilter.FilteringStrategy.UNTAGGED, 0, new String[]{"facet.tags is NULL"});
    }

    private void testTagFilter(final TagFilter tagFilter,
                               final TagFilter.FilteringStrategy expectedFilteringStrategy,
                               final int expectedNumberOfTags,
                               final String[] expectedWhereClauseParts) {
        Assert.assertNotNull(tagFilter);
        Assert.assertNotNull(tagFilter.getTags());
        Assert.assertEquals(expectedNumberOfTags, tagFilter.getTags().size());
        Assert.assertEquals(expectedFilteringStrategy, tagFilter.getFilteringStrategy());
//        Assert.assertEquals(expectedWhereClause, tagFilter.getWhereClause());
        
        String whereClause = tagFilter.getWhereClause();
        for (String expectedWhereClausePart : expectedWhereClauseParts){
        	String message = "Validation of where clause failed. Could not find ["+expectedWhereClausePart+"] in ["+whereClause+"]";
        	Assert.assertTrue(message, whereClause.contains(expectedWhereClausePart));
        }
    }
    
}