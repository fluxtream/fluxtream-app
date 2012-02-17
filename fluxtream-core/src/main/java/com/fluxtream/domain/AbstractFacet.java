package com.fluxtream.domain;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

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
	
	protected abstract void makeFullTextIndexable();
}
