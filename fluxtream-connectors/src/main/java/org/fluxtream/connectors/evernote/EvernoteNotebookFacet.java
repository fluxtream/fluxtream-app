package org.fluxtream.connectors.evernote;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.evernote.edam.type.Publishing;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;

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
    public Publishing publishing;
    public String stack;
    public Boolean published;

    public EvernoteNotebookFacet() { super(); }

    public EvernoteNotebookFacet(long apiKeyId) {super(apiKeyId);}

    @Override
    protected void makeFullTextIndexable() {
        if (name!=null)
            fullTextDescription = name;
    }

}
