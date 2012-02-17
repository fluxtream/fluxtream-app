package com.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fluxtream.Configuration;

@Entity(name="ApiKeyAttribute")
public class ApiKeyAttribute extends AbstractEntity {

	@ManyToOne
	public ApiKey apiKey;

	public String attributeKey;
	
	@Lob
	String attributeValue;

	public int hashcode() {
		return HashCodeBuilder.reflectionHashCode(this, false);
	}
	
	public void setAttributeValue(String value, Configuration env) {
		this.attributeValue = env.encrypt(value);
	}
	
	public boolean equals(Object o1, Object o2) {
		return EqualsBuilder.reflectionEquals(o1, o2);
	}
	
}
