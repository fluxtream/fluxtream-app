package org.fluxtream.core.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Connector;
import com.google.gson.annotations.Expose;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.springframework.util.SerializationUtils;

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
   			query="SELECT apiKey FROM ApiKey apiKey WHERE apiKey.guestId=? AND apiKey.api=? ORDER BY apiKey.id DESC"),
    @NamedQuery( name="apiKeys.all.byApi",
   			query="SELECT apiKey FROM ApiKey apiKey WHERE apiKey.api=?"),
	@NamedQuery( name="apiKey.byAttribute",
			query="SELECT apiKey FROM ApiKey apiKey JOIN apiKey.attributes attr WHERE attr.attributeKey=? AND attr.attributeValue=?")
})
public class ApiKey extends AbstractEntity {

    transient FlxLogger logger = FlxLogger.getLogger(ApiKey.class);

    public enum Status {
        STATUS_UP, STATUS_PERMANENT_FAILURE, STATUS_TRANSIENT_FAILURE, STATUS_OVER_RATE_LIMIT
    }

    public static class PermanentFailReason {
        public final static String NEEDS_REAUTH = "NEEDS_REAUTH";
        public final static String AUTH_REVOKED = "AUTH_REVOKED";
        public final static String CLIENT_ERROR = "CLIENT_ERROR";
        public final static String SERVER_EXCEPTION = "SERVER_EXCEPTION";
        public final static String MANUAL_INTERVENTION = "MANUAL_INTERVENTION";
        public final static String UNKNOWN = "UNKNOWN";
        public final static String DIVIDER = " ";

        public static String clientError(final int code) {
            return new StringBuffer(CLIENT_ERROR).append(DIVIDER).append(code).toString();
        }

        public static String clientError(final int code, String message) {
            StringBuffer sb = new StringBuffer(CLIENT_ERROR).append(DIVIDER).append(code);
            if (message!=null)
                sb.append(DIVIDER).append(message);
            return sb.toString();
        }

        public static String unknownReason(String message) {
            return new StringBuffer(UNKNOWN).append(DIVIDER).append(message).toString();
        }
    }

    @Expose
	@Index(name="guestId_index")
	private long guestId;

    @Type(type="yes_no")
    public boolean synching;

    @Expose
	@Index(name="api_index")
	private int api;
	
	@OneToMany(mappedBy="apiKey", orphanRemoval = true, fetch=FetchType.EAGER, cascade=CascadeType.ALL)
	List<ApiKeyAttribute> attributes = new ArrayList<ApiKeyAttribute>();

    public Status status;

    @Lob
    public String stackTrace;

    public String reason;

    @Lob
    private byte[] settingsStorage;

    @Lob
    private byte[] defaultSettingsStorage;

	public void setGuestId(long guestId) {
		this.guestId = guestId;
	}

	public long getGuestId() {
		return guestId;
	}

    public void setSettings(Object o) {
        settingsStorage = SerializationUtils.serialize(o);
    }

    public void setDefaultSettings(Object o) {
        defaultSettingsStorage = SerializationUtils.serialize(o);
    }

    public Object getSettings() {
        try {
            return SerializationUtils.deserialize(settingsStorage);
        } catch (Throwable e) {
            // let's be robust against class changes
            return null;
        }
    }

    public Object getDefaultSettings() {
        try {
            return SerializationUtils.deserialize(defaultSettingsStorage);
        } catch (Throwable e) {
            // let's be robust against class changes
            return null;
        }
    }

    public void setAttribute(ApiKeyAttribute attr) {
		attr.apiKey = this;
        List<ApiKeyAttribute> toRemove = new ArrayList<ApiKeyAttribute>();
        for (ApiKeyAttribute attribute : attributes) {
            if (attribute.attributeKey==null) {
                logger.warn("null attributeKey for ApiKey: " + guestId + "/" + api);
                toRemove.add(attribute);
                continue;
            }
            if (attribute.attributeKey.equals(attr.attributeKey))
                toRemove.add(attribute);
        }
        for (ApiKeyAttribute attribute : toRemove) {
            attribute.apiKey = null;
            attributes.remove(attribute);
        }
        attributes.add(attr);
	}
	
	private ApiKeyAttribute getAttribute(String key) {
		for (ApiKeyAttribute attr : attributes) {
			if (attr.attributeKey!=null&&attr.attributeKey.equals(key))
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

    public void setConnector(final Connector connector) {
        this.api = connector.value();
    }

    public Status getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
