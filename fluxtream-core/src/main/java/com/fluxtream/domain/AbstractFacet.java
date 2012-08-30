package com.fluxtream.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Query;

import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;

@MappedSuperclass
@Indexed
public abstract class AbstractFacet extends AbstractEntity {
	
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

    public static AbstractFacet getLatestFacet(EntityManager em, Long guestId, Connector connector, ObjectType objType){
        Class facetClass;
        if (objType != null)
            facetClass = objType.facetClass();
        else
            facetClass = connector.facetClass();
        Entity entity = (Entity) facetClass.getAnnotation(Entity.class);
        Query query = em.createQuery("select facet from " + entity.name() + " facet where facet.guestId = " + guestId + " order by facet.end desc limit 1");
        query.setMaxResults(1);
        return (AbstractFacet) query.getResultList().get(0);
    }
	
	protected abstract void makeFullTextIndexable();
}
