package com.fluxtream.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
         * Returns the <code>FilteringStrategy</code> corresponing to the given <code>name</code>, or returns the
         * {@link #getDefault() default} <code>FilteringStrategy</code> if no such strategy exists.
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

    @NotNull
    private List<String> tags = new ArrayList<String>();

    @NotNull
    private final FilteringStrategy filteringStrategy;

    public TagFilter(@Nullable final List<String> tags, @Nullable final FilteringStrategy filteringStrategy) {
        if (tags != null) {
            this.tags.addAll(tags);
        }
        this.filteringStrategy = filteringStrategy == null ? FilteringStrategy.getDefault() : filteringStrategy;
    }
}
