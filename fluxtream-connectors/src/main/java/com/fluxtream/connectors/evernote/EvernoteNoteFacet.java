package com.fluxtream.connectors.evernote;

import javax.persistence.Entity;

/**
 * User: candide
 * Date: 05/12/13
 * Time: 16:45
 */
@Entity(name="Facet_EvernoteNote")
public  class EvernoteNoteFacet extends EvernoteFacet {

    public byte[] contentHash;
    public int contentLength;
    public String content;
    public String title;
    public long created;
    public long updated;
    public String notebookGUID;

    public EvernoteNoteFacet() {
        super();
    }

    public EvernoteNoteFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
    }

}
