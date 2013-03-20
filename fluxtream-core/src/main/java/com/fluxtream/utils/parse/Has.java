package com.fluxtream.utils.parse;

import com.google.gson.JsonObject;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class Has extends AbstractConstraint {

    public Has(final String name) {
        super(name);
    }

    @Override
    public void addToWhereClause(final JsonObject whereClause) {
        JsonObject constraint = new JsonObject();
        constraint.addProperty("$exists", true);
        whereClause.add(name, constraint);
    }
}
