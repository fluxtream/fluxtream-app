package org.fluxtream.core.utils.gson;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import javax.xml.bind.DatatypeConverter;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * @author Candide Kemmler (candide@fluxtream.com)
 */

public class DateDeserializer implements JsonDeserializer<Date> {
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonObject()) {
            final JsonObject asJsonObject = json.getAsJsonObject();
            final JsonElement iso = asJsonObject.get("iso");
            final String asString = iso.getAsString();
            final Calendar calendar = DatatypeConverter.parseDateTime(asString);
            return calendar.getTime();
        } else {
            final String asString = json.getAsString();
            final Calendar calendar = DatatypeConverter.parseDateTime(asString);
            return calendar.getTime();
        }
    }
}
