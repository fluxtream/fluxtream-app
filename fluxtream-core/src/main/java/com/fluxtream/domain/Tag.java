package com.fluxtream.domain;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "Tags")
@NamedQueries({
  @NamedQuery(name = "tags.all", query = "SELECT tag "
                                       + "FROM Tags tag "
                                       + "WHERE tag.guestId=?"),
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
     * Parses the given space-delimited {@link String} of tags, and returns them as a {@link Set} of {@link Tag}
     * objects.  Characters which are considered "illegal" within our system are replaced with an underscore.
     * Characters we consider legal are numbers, letters, space, dash, and underscore.  Note that the {@link Tag}
     * objects will have a <code>null</code> {@link Tag#guestId}.
     */
    public static Set<Tag> parseTags(final String tagsStr) {
        final Set<Tag> tagSet = new HashSet<Tag>();

        if (tagsStr != null && tagsStr.length() > 0) {
            for (final String tagStr : tagsStr.trim().toLowerCase().split(" ")) {
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
     * This method "cleanses" a given tag by trimming whitespace off the ends, forcing it to all-lowercase, and then
     * replacing illegal characters with underscores.  Returns an empty {@link String} if the given <code>tag</code> is
     * <code>null</code>.  Guaranteed to never return <code>null</code>.
     *
     * @see #REGEX_ILLEGAL_CHARACTERS
     */
    public static String cleanse(final String tag) {
        if (tag != null) {
            return tag.trim().toLowerCase().replaceAll(REGEX_ILLEGAL_CHARACTERS, ILLEGAL_CHARACTER_REPLACEMENT);
        }
        return "";
    }
}
