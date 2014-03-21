package org.fluxtream.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class TagFilter {
    private static final Logger LOG = Logger.getLogger(TagFilter.class);

    public enum FilteringStrategy {
        ALL("all"), ANY("any"), NONE("none"), UNTAGGED("untagged");

        @NotNull
        private final String name;

        /**
         * Returns the <code>FilteringStrategy</code> corresponing to the given <code>name</code> (case-insenstive), or
         * returns the {@link #getDefault() default} <code>FilteringStrategy</code> if no such strategy exists.
         */
        @Nullable
        public static FilteringStrategy findByName(@Nullable final String name) {
            if (name != null) {
                try {
                    return FilteringStrategy.valueOf(name.toUpperCase(Locale.ENGLISH));
                }
                catch (IllegalArgumentException ignored) {
                    LOG.info("Unknown FilteringStrategy name [" + name + "], returning default");
                }
            }
            return getDefault();
        }

        /**
         * Returns the {@link #ANY} <code>FilteringStrategy</code>.
         */
        @NotNull
        public static FilteringStrategy getDefault() {
            return ANY;
        }

        private FilteringStrategy(@NotNull final String name) {
            this.name = name;
        }

        @NotNull
        public String getName() {
            return name;
        }
    }

    /**
     * Creates a <code>TagFilter</code> from the given {@link Collection} of tags and {@link FilteringStrategy} using the
     * following rules:
     * <ul>
     *    <li>
     *       If the {@link FilteringStrategy} is <code>null</code>, the {@link FilteringStrategy#getDefault() default}
     *       is used instead.
     *    </li>
     *    <li>
     *       If the given {@link Collection} of tags is non-<code>null</code>, then a cleansed {@link Collection} of
     *       unique tags is created by only adding tags which are non-empty after being
     *       {@link Tag#cleanse(String) cleansed}.
     *    </li>
     *    <li>
     *       If the {@link FilteringStrategy} is {@link FilteringStrategy#UNTAGGED}, then the given {@link Collection}
     *       of <code>tags</code> is ignored and a <code>TagFilter</code> is created with an empty {@link Collection} of
     *       tags.
     *    </li>
     *    <li>
     *       If the {@link FilteringStrategy} is <i>not</i> {@link FilteringStrategy#UNTAGGED}, but the given cleansed
     *       {@link Collection} of <code>tags</code> is <code>null</code> or empty, then this method returns
     *       <code>null</code>.
     *    </li>
     *    <li>
     *       If the {@link FilteringStrategy} is <i>not</i> {@link FilteringStrategy#UNTAGGED}, and the given
     *       {@link Collection} of <code>tags</code> is non-<code>null</code> and non-empty, then this method creates a
     *       <code>TagFilter</code> using the given unique, cleansed <code>tags</code> and {@link FilteringStrategy}.
     *    </li>
     * </ul>
     *
     */
    @Nullable
    public static TagFilter create(@Nullable final Collection<String> tags,
                                   @Nullable final FilteringStrategy requestedFilteringStrategy) {
        // make sure the filtering strategy is non-null, choosing the default if necessary
        final FilteringStrategy filteringStrategy = requestedFilteringStrategy == null ? FilteringStrategy.getDefault() : requestedFilteringStrategy;

        if (FilteringStrategy.UNTAGGED.equals(filteringStrategy)) {
            // filter for untagged items
            return new TagFilter(null, FilteringStrategy.UNTAGGED);
        }
        else {
            if (tags != null) {
                final Set<String> cleansedTags = new HashSet<String>();
                for (final String tag : tags) {
                    final String cleansedTag = Tag.cleanse(tag);
                    if (cleansedTag.length() > 0) {
                        cleansedTags.add(cleansedTag);
                    }
                }
                if (!cleansedTags.isEmpty()) {
                    return new TagFilter(cleansedTags, filteringStrategy);
                }
            }
        }
        return null;
    }

    @NotNull
    private Set<String> tags = new HashSet<String>();

    @NotNull
    private final FilteringStrategy filteringStrategy;

    private TagFilter(@Nullable final Collection<String> tags, @NotNull final FilteringStrategy filteringStrategy) {
        if (tags != null) {
            this.tags.addAll(tags);
        }
        this.filteringStrategy = filteringStrategy;
    }

    /** Returns an {@link Collections#unmodifiableSet(Set) unmodifiable set} of the tags in this TagFilter. */
    @NotNull
    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /** Returns the {@link FilteringStrategy}. */
    @NotNull
    public FilteringStrategy getFilteringStrategy() {
        return filteringStrategy;
    }

    @NotNull
    public String getWhereClause() {
        List<String> likeClauses = null;
        if (!FilteringStrategy.UNTAGGED.equals(filteringStrategy)) {
            likeClauses = new ArrayList<String>();
            final String notClause = FilteringStrategy.NONE.equals(filteringStrategy) ? "NOT " : "";
            for (final String tag : tags) {
                likeClauses.add("facet.tags " + notClause + "like '%," + tag + ",%'");
            }
        }
        switch (filteringStrategy) {
            case ANY:
                return StringUtils.join(likeClauses, " OR ");
            case ALL:
                return StringUtils.join(likeClauses, " AND ");
            case NONE:
                return "facet.tags is NULL OR (" + StringUtils.join(likeClauses, " AND ") + ")";
            case UNTAGGED:
                return "facet.tags is NULL";
        }
        return "";
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TagFilter{");
        sb.append("tags=").append(tags);
        sb.append(", filteringStrategy=").append(filteringStrategy);
        sb.append('}');
        return sb.toString();
    }
}
