package org.fluxtream.api.gson;

import java.lang.reflect.Type;
import org.fluxtream.connectors.updaters.UpdateInfo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class UpdateInfoSerializer implements JsonSerializer<UpdateInfo> {

    @Override
    public JsonElement serialize(final UpdateInfo updateInfo, final Type typeOfSrc, final JsonSerializationContext context) {
        JsonObject root = new JsonObject();
        root.addProperty("guestId", updateInfo.getGuestId());
        root.addProperty("connector", updateInfo.apiKey.getConnector().getName());
        root.addProperty("updateType", updateInfo.getUpdateType().toString());
        root.addProperty("objectTypes", updateInfo.objectTypes().toString());

        return root;
    }

}
