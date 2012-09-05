package com.fluxtream.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Index;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;

@Entity(name="ApiKey")
@NamedQueries ( {
	@NamedQuery( name="apiKeys.byConnector",
			query="SELECT apiKey FROM ApiKey apiKey WHERE apiKey.api=?"),
	@NamedQuery( name="apiKeys.delete.all",
			query="DELETE FROM ApiKey apiKey WHERE apiKey.guestId=?"),
	@NamedQuery( name="apiKeys.all",
			query="SELECT apiKey FROM ApiKey apiKey WHERE apiKey.guestId=?"),
	@NamedQuery( name="apiKey.count.byApi",
			query="SELECT COUNT(apiKey) FROM ApiKey apiKey WHERE apiKey.api=?"),
	@NamedQuery( name="apiKey.byApi",
			query="SELECT apiKey FROM ApiKey apiKey WHERE apiKey.guestId=? AND apiKey.api=?"),
	@NamedQuery( name="apiKey.byAttribute",
			query="SELECT apiKey FROM ApiKey apiKey JOIN apiKey.attributes attr WHERE attr.attributeKey=? AND attr.attributeValue=?")
})
public class ApiKey extends AbstractEntity {

	@Index(name="guestId_index")
	private long guestId;
	
	@Index(name="api_index")
	private int api;
	
	@OneToMany(mappedBy="apiKey", fetch=FetchType.EAGER, cascade=CascadeType.ALL)
	Set<ApiKeyAttribute> attributes = new HashSet<ApiKeyAttribute>();

	public void setGuestId(long guestId) {
		this.guestId = guestId;
	}

	public long getGuestId() {
		return guestId;
	}

	public void setAttribute(ApiKeyAttribute attr) {
		attr.apiKey = this;
        List<ApiKeyAttribute> toRemove = new ArrayList<ApiKeyAttribute>();
        for (ApiKeyAttribute attribute : attributes) {
            if (attribute.attributeKey.equals(attr.attributeKey))
                toRemove.add(attribute);
        }
        for (ApiKeyAttribute attribute : toRemove)
            attributes.remove(attribute);
        attributes.add(attr);
	}
	
	private ApiKeyAttribute getAttribute(String key) {
		for (ApiKeyAttribute attr : attributes) {
			if (attr.attributeKey.equals(key))
				return attr;
		}
		return null;
	}
	
	public String getAttributeValue(String key, Configuration env) {
		ApiKeyAttribute attribute = getAttribute(key);
		if (attribute!=null) {
			String attValue = env.decrypt(attribute.attributeValue);
			return attValue;
		}
		return null;
	}

    public Map<String,String> getAttributes(Configuration env) {
        Map<String,String> atts = new HashMap<String, String>();
        if (attributes==null) return atts;
        for (ApiKeyAttribute attribute : attributes) {
            String attValue = env.decrypt(attribute.attributeValue);
            atts.put(attribute.attributeKey, attValue);
        }
        return atts;
    }

	public void removeAttribute(String key) {
		ApiKeyAttribute attribute = getAttribute(key);
		if (attribute==null) return;
		attribute.attributeKey = null;
		attributes.remove(attribute);
	}

	public void setConnector(Connector api) {
		this.api = api.value();
	}
	
	public boolean equals(Object o) {
		if (! (o instanceof ApiKey))
			return false;
		ApiKey key = (ApiKey) o;
		return key.guestId == guestId
				&& key.api == api;
	}
	
	public String toString() {
		return getConnector().getName();
	}

	public Connector getConnector() {
		return Connector.fromValue(api);
	}
}
