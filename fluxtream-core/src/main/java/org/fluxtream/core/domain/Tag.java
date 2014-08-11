package org.fluxtream.core.domain;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "Tags")
@NamedQueries({
  @NamedQuery(name = "tags.all", query = "SELECT tag "
                                       + "FROM Tags tag "
                                       + "WHERE tag.guestId=? ORDER BY tag.name"),
  @NamedQuery(name = "tags.byName", query = "SELECT tag "
                                          + "FROM Tags tag "
                                          + "WHERE tag.guestId=? "
                                          + "AND tag.name=?"),
  @NamedQuery(name = "tags.delete.all",
              query = "DELETE FROM Tags tag WHERE tag.guestId=?")
})
public class Tag extends AbstractEntity {
    /** Regex for illegal characters (it's simply the negation of the legal characters) */
    public static final String REGEX_ILLEGAL_CHARACTERS = "[^a-zA-Z0-9-_]";

    /** Character used to replace illegal characters */
    public static final String ILLEGAL_CHARACTER_REPLACEMENT = "_";

    public static final char SPACE_DELIMITER = ' ';
    public static final char COMMA_DELIMITER = ',';

    public Tag() {}

    public long guestId;

    @Index(name = "name")
    public String name;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Tag");
        sb.append("{guestId=").append(guestId);
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Tag tag = (Tag)o;

        if (guestId != tag.guestId) {
            return false;
        }
        if (name != null ? !name.equals(tag.name) : tag.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int)(guestId ^ (guestId >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    /**
     * Parses the given {@link String} of tags, delimited by the given <code>delimiter</code>, and returns them as a
     * {@link Set} of {@link Tag} objects.  Characters which are considered "illegal" within our system are replaced
     * with an underscore. Characters we consider legal are numbers, letters, space, dash, and underscore.  Note that
     * the {@link Tag} objects in the returned {@link Set} will have a <code>null</code> {@link Tag#guestId}.
     */
    @NotNull
    public static Set<Tag> parseTags(@Nullable final String tagsStr, final char delimiter) {
        final Set<Tag> tagSet = new HashSet<Tag>();

        if (tagsStr != null && tagsStr.length() > 0) {
            for (final String tagStr : tagsStr.trim().toLowerCase().split(String.valueOf(delimiter))) {
                final String cleanTagStr = cleanse(tagStr);
                if (cleanTagStr.length() > 0) {
                    final Tag tag = new Tag();
                    tag.name = cleanTagStr;
                    tagSet.add(tag);
                }
            }
        }

        return tagSet;
    }

    /**
     *
     * Parses the given {@link String} of tags, delimited by the given <code>delimiter</code>, and returns them as a
     * {@link Set} of {@link String} objects.  Characters which are considered "illegal" within our system are replaced
     * with an underscore. Characters we consider legal are numbers, letters, space, dash, and underscore.
     */
    @NotNull
    public static Set<String> parseTagsIntoStrings(@Nullable final String tagsStr, final char delimiter) {
        final Set<String> tagSet = new HashSet<String>();

        if (tagsStr != null && tagsStr.length() > 0) {
            for (final String tagStr : tagsStr.trim().toLowerCase().split(String.valueOf(delimiter))) {
                final String cleanTagStr = cleanse(tagStr);
                if (cleanTagStr.length() > 0) {
                    tagSet.add(cleanTagStr);
                }
            }
        }

        return tagSet;
    }

    /**
     * This method "cleanses" a given tag by trimming whitespace off the ends, forcing it to all-lowercase, and then
     * replacing illegal characters with underscores.  Returns an empty {@link String} if the given <code>tag</code> is
     * <code>null</code>.  Guaranteed to never return <code>null</code>.
     *
     * @see #REGEX_ILLEGAL_CHARACTERS
     */
    @NotNull
    public static String cleanse(@Nullable final String tag) {
        if (tag != null) {
            return tag.trim().toLowerCase().replaceAll(REGEX_ILLEGAL_CHARACTERS, ILLEGAL_CHARACTER_REPLACEMENT);
        }
        return "";
    }
}
