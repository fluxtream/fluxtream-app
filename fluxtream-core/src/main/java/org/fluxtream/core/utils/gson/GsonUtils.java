package org.fluxtream.core.utils.gson;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class GsonUtils  {

    static Gson gson;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new DateSerializer());
        gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
        gson = gsonBuilder.create();
    }

    public static <T> List<T> cast(String result, Class<T> clazz, Type listType) {
        JSONObject resultsJson = JSONObject.fromObject(result);
        final JSONArray results = resultsJson.getJSONArray("results");
        List<T> items = gson.fromJson(results.toString(), listType);
        return items;
    }

}
