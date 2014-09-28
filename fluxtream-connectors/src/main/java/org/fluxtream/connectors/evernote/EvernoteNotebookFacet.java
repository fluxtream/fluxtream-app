package org.fluxtream.connectors.evernote;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 05/12/13
 * Time: 17:00
 */
@Entity(name="Facet_EvernoteNotebook")
@ObjectTypeSpec(name = "notebook", value = 2, prettyname = "Notebook", clientFacet = false)
@NamedQueries({
      @NamedQuery(name = "evernote.notebooks.byApiKeyId", query = "SELECT facet FROM Facet_EvernoteNotebook facet WHERE facet.apiKeyId=?")
})
public class EvernoteNotebookFacet extends EvernoteFacet {

    public String name;
    public Boolean defaultNotebook;
    public Long serviceCreated;
    public Long serviceUpdated;

    public String stack;

    public Boolean published;
    public Integer publishingNoteOrderValue;
    public String publishingUri;

    @Lob
    public String publishingPublicDescription;

    public EvernoteNotebookFacet() { super(); }

    public EvernoteNotebookFacet(long apiKeyId) {super(apiKeyId);}

    @Override
    protected void makeFullTextIndexable() {
        if (name!=null)
            fullTextDescription = name;
    }

}
