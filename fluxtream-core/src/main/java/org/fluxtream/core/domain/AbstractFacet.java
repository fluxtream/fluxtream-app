package org.fluxtream.core.domain;

import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.utils.JPAUtils;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTimeUtils;

import javax.persistence.*;
import java.util.*;

@MappedSuperclass
@Indexed
public abstract class AbstractFacet extends AbstractEntity {
    private static final FlxLogger LOG_DEBUG = FlxLogger.getLogger("Fluxtream");

    protected static final String TAG_DELIMITER = ",";

    public AbstractFacet() {
        this.timeUpdated = System.currentTimeMillis();
        figureOutObjectType();
    }

    private void figureOutObjectType() {
        ObjectTypeSpec objectType = this.getClass().getAnnotation(ObjectTypeSpec.class);
        if (objectType!=null)
            this.objectType = objectType.value();
        else
            this.objectType = -1;
    }

    public AbstractFacet(Long apiKeyId) {
        this.timeUpdated = System.currentTimeMillis();
        this.apiKeyId = apiKeyId;
        figureOutObjectType();
	}

    @Index(name = "apiKey")
    public Long apiKeyId;

    @Index(name="guestId_index")
	@Field
	public long guestId;
	
	@Type(type="yes_no")
	@Index(name="isEmpty_index")
	public boolean isEmpty = false;
	
	@Index(name="timeUpdated_index")
	public long timeUpdated;
	
	@Field(index=org.hibernate.search.annotations.Index.UN_TOKENIZED, store=Store.YES)
	@Index(name="start_index")
	public long start;
	
	@Index(name="end_index")
	public long end;
	@Index(name="api_index")
	public int api;
	@Index(name="objectType_index")
	public int objectType;

    /**
     * A string representation of the tags for this facet.  You should NEVER set this field directly.  Instead, always
     * use the {@link #addTags} method which sets both this and the {@link #tagSet} fields.
     */
	@Lob
    public String tags;

    /**
     * A {@link Set} representation of the tags for this facet.  You should NEVER set this field directly.  Instead,
     * always use the {@link #addTags} method which sets both this and the {@link #tags} fields.
     */
    public transient Set<Tag> tagSet;

	@Lob
	@Field(index=org.hibernate.search.annotations.Index.TOKENIZED, store=Store.YES)
	public String comment;
	
	@Field(index=org.hibernate.search.annotations.Index.TOKENIZED, store=Store.YES)
	@Lob
	public String fullTextDescription;

    @PostLoad
    void loadTags() {
        if (tags == null || tags.equals("")) {
            return;
        }
        StringTokenizer st = new StringTokenizer(tags,", \t\n\r\f");
        while (st.hasMoreTokens()) {
            String tag = st.nextToken().trim();
            if (tag.length() > 0) {
                addTag(tag);
            }
        }
    }

    private void addTag(final String tagName) {
        if (tagName != null && tagName.length() > 0) {
            if (tagSet == null) {
                tagSet = new HashSet<Tag>();
            }
            Tag tag = new Tag();
            tag.name = tagName;
            tagSet.add(tag);
        }
    }

    protected void persistTags() {
        buildTagsStringFromTagsSet();
    }

    private void buildTagsStringFromTagsSet() {
        if (tagSet == null) {
            return;
        }
        if (tagSet.size() > 0) {
            final StringBuilder sb = new StringBuilder(TAG_DELIMITER);
            for (final Tag tag : tagSet) {
                if (tag.name.length() > 0) {
                    sb.append(tag.name).append(TAG_DELIMITER);
                }
            }
            if (sb.length() > 1) {
                tags = sb.toString();
            }
        }
        else {
            tags = "";
        }
    }

    @PrePersist
    @PreUpdate
    protected void setFullTextDescription() {
        this.fullTextDescription = null;
        makeFullTextIndexable();
        if (this.comment != null) {
            if (this.fullTextDescription == null) {
                this.fullTextDescription = "";
            }
            this.fullTextDescription += " " + this.comment;
            this.fullTextDescription = this.fullTextDescription.trim();
        }
        this.timeUpdated = DateTimeUtils.currentTimeMillis();
        persistTags();
    }

    /** Clears this instance's tags. */
    public void clearTags() {
        if (tagSet != null) {
            tagSet.clear();
        }
        tags = "";
    }

    /**
     * Clears this instance's existing tags, parses the given tags {@link String} which is delimited by the given
     * <code>delimiter</code>, replacing illegal characters with an underscore, and adds them to this instance's
     * {@link #tags} and {@link #tagSet} fields.  One should ALWAYS use this method instead of directly setting the
     * member fields.
     *
     * @see Tag#parseTags(String, char)
     */
    public void addTags(final String tagsStr, final char delimiter) {
        if (tagsStr != null && tagsStr.length() > 0) {
            // create the Set if necessary
            if (tagSet == null) {
                tagSet = new HashSet<Tag>();
            }

            tagSet.addAll(Tag.parseTags(tagsStr, delimiter));

            // build the String representation
            buildTagsStringFromTagsSet();
        }
    }

    /** Returns an {@link Collections#unmodifiableSet(Set) unmodifiable Set} of the tags for this facet. */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tagSet);
    }

    /** Returns an {@link SortedSet} of the tags for this facet. Modifying the returned set will have no effect on the facet's tags. */
    public SortedSet<String> getTagsAsStrings() {
        final SortedSet<String> tagStrings = new TreeSet<String>();
        if ((tagSet != null) && (!tagSet.isEmpty())) {
            for (final Tag tag : tagSet) {
                if (tag != null && tag.name.length() > 0) {
                    tagStrings.add(tag.name);
                }
            }
        }

        return tagStrings;
    }

    public boolean hasTags() {
        return tagSet != null && tagSet.size() > 0;
    }

    public static AbstractFacet getOldestFacet(EntityManager em, ApiKey apiKey, ObjectType objType) {
        return getOldestOrLatestFacet(em, apiKey, objType, "ASC");
    }

    public static AbstractFacet getLatestFacet(EntityManager em, ApiKey apiKey, ObjectType objType){
        return getOldestOrLatestFacet(em, apiKey, objType, "DESC");
    }

    private static AbstractFacet getOldestOrLatestFacet(EntityManager em, ApiKey apiKey, ObjectType objType, String sortOrder) {
        Class facetClass;
        if (objType != null) {
            facetClass = objType.facetClass();
        }
        else {
            facetClass = apiKey.getConnector().facetClass();
        }
        final String entityName = JPAUtils.getEntityName(facetClass);
        String queryString = String.format("SELECT * FROM %s USE INDEX (apiKey) WHERE apiKeyId=? ORDER BY end %s", entityName, sortOrder);
        // this is a temporary hack before we bite the bullet and add the index on all tables - for now only
        // the location table is problematic
        if (entityName.equals("Facet_Location"))
            queryString = String.format("SELECT * FROM %s USE INDEX (apiKeyIdEnd) WHERE apiKeyId=? ORDER BY end %s", entityName, sortOrder);
        Query query = em.createNativeQuery(queryString, facetClass);
        query.setParameter(1, apiKey.getId());
        query.setMaxResults(1);
        final List<? extends AbstractFacet> resultList = query.getResultList();
        if (resultList != null && resultList.size() > 0) {
            return resultList.get(0);
        }
        return null;
    }

    public static List<AbstractFacet> getFacetsBefore(EntityManager em,
                                                      ApiKey apiKey,
                                                      ObjectType objType,
                                                      Long timeInMillis,
                                                      Integer desiredCount) {
        return getFacetsBefore(em, apiKey, objType, timeInMillis, desiredCount, null);
    }

    public static List<AbstractFacet> getFacetsAfter(EntityManager em,
                                                     ApiKey apiKey,
                                                     ObjectType objType,
                                                     Long timeInMillis,
                                                     Integer desiredCount){
        return getFacetsAfter(em, apiKey, objType, timeInMillis, desiredCount, null);
    }

    public static List<AbstractFacet> getFacetsBefore(EntityManager em,
                                                      ApiKey apiKey,
                                                      ObjectType objType,
                                                      Long timeInMillis,
                                                      Integer desiredCount,
                                                      @Nullable final TagFilter tagFilter) {
        final Class facetClass = getFacetClass(apiKey, objType);
        final String entityName = JPAUtils.getEntityName(facetClass);
        final String additionalWhereClause = (tagFilter == null) ? "" : " AND (" + tagFilter.getWhereClause() + ")";
        String queryString = String.format("SELECT * FROM %s facet USE INDEX (apiKey) WHERE apiKeyId=? AND start <=? %s ORDER BY start DESC",
                                           entityName, additionalWhereClause);
        final Query query = em.createNativeQuery(queryString, facetClass);
        query.setParameter(1, apiKey.getId());
        query.setParameter(2, timeInMillis);
        query.setMaxResults(desiredCount);
        return query.getResultList();
    }

    public static List<AbstractFacet> getFacetsAfter(EntityManager em,
                                                     ApiKey apiKey,
                                                     ObjectType objType,
                                                     Long timeInMillis,
                                                     Integer desiredCount,
                                                     @Nullable final TagFilter tagFilter){
        final Class facetClass = getFacetClass(apiKey, objType);
        final String entityName = JPAUtils.getEntityName(facetClass);
        final String additionalWhereClause = (tagFilter == null) ? "" : " AND (" + tagFilter.getWhereClause() + ")";
        String queryString = String.format("SELECT * FROM %s facet USE INDEX (apiKey) WHERE apiKeyId=? AND start >=? %s ORDER BY start ASC",
                                           entityName, additionalWhereClause);
        final Query query = em.createNativeQuery(queryString, facetClass);
        query.setParameter(1, apiKey.getId());
        query.setParameter(2, timeInMillis);
        query.setMaxResults(desiredCount);
        return (List<AbstractFacet>)query.getResultList();
    }

    private static Class getFacetClass(final ApiKey apiKey, final ObjectType objType) {
        final Class facetClass;
        if (objType != null) {
            facetClass = objType.facetClass();
        }
        else {
            facetClass = apiKey.getConnector().facetClass();
        }
        return facetClass;
    }

    protected abstract void makeFullTextIndexable();
}
