package com.fluxtream.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Query;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

@MappedSuperclass
@Indexed
public abstract class AbstractFacet extends AbstractEntity {

    private static final Logger LOG = Logger.getLogger(AbstractFacet.class);

    public AbstractFacet() {
		ObjectTypeSpec objectType = this.getClass().getAnnotation(ObjectTypeSpec.class);
		if (objectType!=null)
			this.objectType = objectType.value();
		else
			this.objectType = -1;
	}


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
	
	@Lob
    public String tags;

    public transient List<Tag> tagsList;

	@Lob
	@Field(index=org.hibernate.search.annotations.Index.TOKENIZED, store=Store.YES)
	public String comment;
	
	@Field(index=org.hibernate.search.annotations.Index.TOKENIZED, store=Store.YES)
	@Lob
	public String fullTextDescription;

    @PostLoad
    void loadTags() {
        StringTokenizer st = new StringTokenizer(tags);
        while(st.hasMoreTokens()) {
            String tag = st.nextToken();
            if (tag.length()>0)
                addTag(tag);
        }
    }

    private void addTag(final String tagName) {
        if (tagsList==null)
            tagsList = new ArrayList<Tag>();
        Tag tag = new Tag();
        tag.name = tagName;
        tagsList.add(tag);
    }

    @PrePersist
    void persistTags() {
        StringBuilder sb = new StringBuilder(",");
        for (Tag tag : tagsList)
            sb.append(tag.name).append(",");
        if (sb.length()>1)
            tags = sb.toString();
        else
            tags = "";
    }
	
	@PrePersist @PreUpdate
	protected void setFullTextDescription() {
		this.fullTextDescription = null;
		makeFullTextIndexable();
		if (this.comment!=null) {
			if (this.fullTextDescription==null)
				this.fullTextDescription = "";
			this.fullTextDescription += this.comment;
		}
	}

    public static AbstractFacet getOldestFacet(EntityManager em, Long guestId, Connector connector, ObjectType objType) {
        return getOldestOrLatestFacet(em, guestId, connector, objType, "asc");
    }

    public static AbstractFacet getLatestFacet(EntityManager em, Long guestId, Connector connector, ObjectType objType){
        return getOldestOrLatestFacet(em, guestId, connector, objType, "desc");
    }

    private static AbstractFacet getOldestOrLatestFacet(EntityManager em, Long guestId, Connector connector, ObjectType objType, String sortOrder) {
        Class facetClass;
        if (objType != null) {
            facetClass = objType.facetClass();
        }
        else {
            facetClass = connector.facetClass();
        }
        Entity entity = (Entity)facetClass.getAnnotation(Entity.class);
        Query query = em.createQuery("select facet from " + entity.name() + " facet where facet.guestId = " + guestId + " order by facet.end " + sortOrder + " limit 1");
        query.setMaxResults(1);
        return (AbstractFacet)query.getResultList().get(0);
    }

    public static List<AbstractFacet> getFacetsBefore(EntityManager em,
                                                      Long guestId,
                                                      Connector connector,
                                                      ObjectType objType,
                                                      Long timeInMillis,
                                                      Integer desiredCount) {
        final Class facetClass;
        if (objType != null) {
            facetClass = objType.facetClass();
        }
        else {
            facetClass = connector.facetClass();
        }
        final Entity entity = (Entity)facetClass.getAnnotation(Entity.class);
        final Query query = em.createQuery("select facet from " + entity.name() + " facet where facet.guestId = " + guestId + " and facet.start <= " + timeInMillis + " order by facet.start desc limit " + desiredCount);
        query.setMaxResults(desiredCount);
        return (List<AbstractFacet>)query.getResultList();
    }

    public static List<AbstractFacet> getFacetsAfter(EntityManager em,
                                                     Long guestId,
                                                     Connector connector,
                                                     ObjectType objType,
                                                     Long timeInMillis,
                                                     Integer desiredCount){
        final Class facetClass;
        if (objType != null) {
            facetClass = objType.facetClass();
        }
        else {
            facetClass = connector.facetClass();
        }
        final Entity entity = (Entity)facetClass.getAnnotation(Entity.class);
        final Query query = em.createQuery("select facet from " + entity.name() + " facet where facet.guestId = " + guestId + " and facet.start >= " + timeInMillis + " order by facet.start asc limit " + desiredCount);
        query.setMaxResults(desiredCount);
        return (List<AbstractFacet>)query.getResultList();
    }

    protected abstract void makeFullTextIndexable();
}
