package com.fluxtream.utils.parse;

import java.util.Date;
import com.fluxtream.utils.gson.DateSerializer;
import com.google.gson.JsonObject;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class Eq extends AbstractConstraint  {

    public Eq(final String name, final Object value) {
        super(name, value);
    }

    @Override
    public void addToWhereClause(final JsonObject whereClause) {
        if (value instanceof String)
            whereClause.addProperty(name, (String) value);
        else if (value instanceof Number)
            whereClause.addProperty(name, (Number) value);
        else if (value instanceof Boolean)
            whereClause.addProperty(name, (Boolean) value);
        else if (value instanceof Date)
            whereClause.add(name, new DateSerializer().getJsonElement((Date) value));
    }
}
