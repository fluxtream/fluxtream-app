package com.fluxtream.utils.parse;

import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class In extends AbstractConstraint {

    String[] values;

    public In(final String name, String[] values) {
        super(name);
        this.values = values;
    }

    public In(final String name, List<String> values) {
        super(name);
        this.values = values.toArray(new String[values.size()]);
    }

    @Override
    public void addToWhereClause(final JsonObject whereClause) {
        JsonObject constraint = new JsonObject();
        constraint.add("$in", toJsonArray(values));
        whereClause.add(name, constraint);
    }

    private JsonArray toJsonArray(final String[] values) {
        JsonArray array = new JsonArray();
        for (String value : values)
            array.add(new JsonPrimitive(value));
        return array;
    }
}
