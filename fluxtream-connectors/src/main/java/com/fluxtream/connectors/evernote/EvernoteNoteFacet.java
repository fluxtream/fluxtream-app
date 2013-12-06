package com.fluxtream.connectors.evernote;

import javax.persistence.Entity;
import javax.persistence.Lob;

/**
 * User: candide
 * Date: 05/12/13
 * Time: 16:45
 */
@Entity(name="Facet_EvernoteNote")
public  class EvernoteNoteFacet extends EvernoteFacet {

    public byte[] contentHash;
    public Integer contentLength;
    public String content;
    public String title;
    public Long created;
    public Long updated;
    public Long deleted;
    public String notebookGuid;
    public Boolean active;

    @Lob
    public String resourcesStorage;

    public EvernoteNoteFacet() {
        super();
    }

    public EvernoteNoteFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
        StringBuilder sb = new StringBuilder();

        if (title!=null)
            sb.append(title);
        if (content!=null)
            sb.append(" ").append(content);
    }

}
