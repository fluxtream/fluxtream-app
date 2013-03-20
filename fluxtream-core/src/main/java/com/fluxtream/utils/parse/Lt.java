package com.fluxtream.utils.parse;

import java.util.Date;
import com.google.gson.JsonObject;
import com.via.services.parse.gson.DateSerializer;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class Lt extends AbstractConstraint {

    public Lt(final String name, final Object value) {
        super(name, value);
    }

    @Override
    public void addToWhereClause(final JsonObject whereClause) {
        JsonObject constraint = new JsonObject();
        if (whereClause.has(name))
            constraint = (JsonObject)whereClause.get(name);
        if (value instanceof String)
            constraint.addProperty("$lt", (String) value);
        else if (value instanceof Number)
            constraint.addProperty("$lt", (Number) value);
        else if (value instanceof Boolean)
            constraint.addProperty("$lt", (Boolean)value);
        else if (value instanceof Date)
            constraint.add("$lt", new DateSerializer().getJsonElement((Date) value));
        whereClause.add(name, constraint);
    }

}
