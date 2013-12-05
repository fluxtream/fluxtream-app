package com.fluxtream.connectors.evernote;

import javax.persistence.Entity;

/**
 * User: candide
 * Date: 05/12/13
 * Time: 16:43
 */
@Entity(name="Facet_EvernoteTag")
public class EvernoteTagFacet extends EvernoteFacet {

    public String name;

    public EvernoteTagFacet() { super(); }

    public EvernoteTagFacet(long apiKeyId) {super(apiKeyId);}

    @Override
    protected void makeFullTextIndexable() {
        fullTextDescription = name;
    }
}
