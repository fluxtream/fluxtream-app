package com.fluxtream.connectors.evernote;

import javax.persistence.Entity;
import javax.persistence.Lob;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 05/12/13
 * Time: 16:45
 */
@Entity(name="Facet_EvernoteNote")
@ObjectTypeSpec(name = "note", value = 4, prettyname = "Note", visibleClause = "facet.deleted IS NULL")
public  class EvernoteNoteFacet extends EvernoteFacet {

    public byte[] contentHash;
    public Integer contentLength;

    @Lob
    public String content;

    @Lob
    public String title;

    public Long created;
    public Long updated;
    public Long deleted;
    public String notebookGuid;
    public Boolean active;

    @Lob
    public String htmlContent;

    public Double altitude;
    @Lob
    public String author;
    public String contentClass;
    public Integer creatorId;
    @Lob
    public String lastEditedBy;
    public Integer lastEditorId;
    public Double latitude;
    public Double longitude;

    @Lob
    public String placeName;
    public Long reminderDoneTime;
    public Long reminderOrder;
    public Long reminderTime;
    public Long shareDate;
    @Lob
    public String source;
    @Lob
    public String sourceApplication;
    @Lob
    public String sourceURL;
    public Long subjectDate;


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
        fullTextDescription = sb.toString();
    }

}
