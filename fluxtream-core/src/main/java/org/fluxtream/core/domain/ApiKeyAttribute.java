package org.fluxtream.core.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import org.fluxtream.core.Configuration;

@Entity(name="ApiKeyAttribute")
@NamedQueries( {
   @NamedQuery( name="apiKeyAttribute.byKeyAndConnector",
        query="SELECT att FROM ApiKeyAttribute att WHERE att.apiKey=? AND att.attributeKey=?")
})
public class ApiKeyAttribute extends AbstractEntity {

    public static final String MIN_TIME_KEY = "minTime";
    public static final String MAX_TIME_KEY = "maxTime";
    public static final String LAST_SYNC_TIME_KEY = "lastSyncTime";

    @ManyToOne
	public ApiKey apiKey;

	public String attributeKey;
	
	@Lob
	String attributeValue;

	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this, false);
	}
	
	public void setAttributeValue(String value, Configuration env) {
		this.attributeValue = env.encrypt(value);
	}
	
	public boolean equals(Object o1, Object o2) {
		return EqualsBuilder.reflectionEquals(o1, o2);
	}
	
}
