package org.fluxtream.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import org.fluxtream.Configuration;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

@Entity(name = "Settings")
@NamedQueries({
	@NamedQuery(name = "settings.byGuestId",
		query = "SELECT settings FROM Settings settings WHERE settings.guestId=?"),
	@NamedQuery(name = "settings.delete.all",
		query = "DELETE FROM Settings settings WHERE settings.guestId=?")
})
public class GuestSettings extends AbstractEntity {

    public transient Configuration config;

	public GuestSettings() {
	}

    public void createMessageDisplayCounters() {
        messageDisplayCounters = new HashMap<String,Integer>();
        saveMessageDisplayCounters();
    }

    public Map<String, Integer> getMessageDisplayCounters() {
        return messageDisplayCounters;
    }

    public enum WeightMeasureUnit {
		SI, POUNDS, STONES
	}

	public enum LengthMeasureUnit {
		SI, FEET_INCHES
	}

	public enum DistanceMeasureUnit {
		SI, MILES_YARDS
	}
	
	public enum TemperatureUnit {
		CELSIUS, FAHRENHEIT
	}

    private transient Map<String, Integer> messageDisplayCounters;

    @Lob
    public String messageDisplayCountersStorage;

    public void saveMessageDisplayCounters() {
        if (messageDisplayCounters==null) return;
        ObjectMapper mapper = new ObjectMapper();
        try {
            messageDisplayCountersStorage = mapper.writeValueAsString(messageDisplayCounters) ;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMessageDisplayCounter(String messageName, int counter) {
        messageDisplayCounters.put(messageName, counter);
        saveMessageDisplayCounters();
    }

    public Integer getMessageDisplayCounter(String messageName) {
        if (messageDisplayCounters==null) return null;
        return messageDisplayCounters.get(messageName);
    }

    @PostLoad
    public void loadMessageDisplayCounters() {
        if (messageDisplayCountersStorage ==null) return;
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        TypeReference<HashMap<String,Object>> typeRef
                = new TypeReference<HashMap<String,Object>>() {};
        try {
            messageDisplayCounters
                    = mapper.readValue(messageDisplayCountersStorage, typeRef);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

	public TemperatureUnit temperatureUnit = TemperatureUnit.FAHRENHEIT;

	public WeightMeasureUnit weightMeasureUnit = WeightMeasureUnit.POUNDS;

	public LengthMeasureUnit lengthMeasureUnit = LengthMeasureUnit.FEET_INCHES;

	public DistanceMeasureUnit distanceMeasureUnit = DistanceMeasureUnit.MILES_YARDS;

	public long guestId;

}
