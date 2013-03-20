package com.fluxtream.utils.parse;

import com.google.gson.JsonObject;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public abstract class AbstractConstraint {

    protected String name;
    protected Object value;

    public AbstractConstraint(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public AbstractConstraint(String name) {
        this.name = name;
    }

    public abstract void addToWhereClause(JsonObject whereClause);

    public String getName() {
        return name;
    }
}
