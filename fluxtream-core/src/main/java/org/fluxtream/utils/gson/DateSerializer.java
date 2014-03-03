package org.fluxtream.utils.gson;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import javax.xml.bind.DatatypeConverter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * @author Candide Kemmler (candide@fluxtream.com)
 */

public class DateSerializer implements JsonSerializer<Date> {

    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        return getJsonElement(src);
    }

    public JsonElement getJsonElement(final Date src) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("__type", "Date");
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(src);
        final String isoTime = DatatypeConverter.printDateTime(calendar);
        jsonObject.addProperty("iso", isoTime);
        return jsonObject;
    }
}