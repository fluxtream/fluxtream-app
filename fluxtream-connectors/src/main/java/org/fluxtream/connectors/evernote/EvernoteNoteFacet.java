package org.fluxtream.connectors.evernote;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Lob;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.apache.commons.lang.StringUtils;

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

    @Lob
    public String tagGuidsStorage;

    public EvernoteNoteFacet() {
        super();
    }

    public EvernoteNoteFacet(long apiKeyId) {
        super(apiKeyId);
    }

    public String[] getTagGuids() {
        return StringUtils.split(tagGuidsStorage, ",");
    }

    public void setTagGuids(List<String> guids) {
        tagGuidsStorage = StringUtils.join(guids, ",");
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
