package com.fluxtream.connectors.evernote;

import javax.persistence.Entity;
import com.evernote.edam.type.Publishing;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 05/12/13
 * Time: 17:00
 */
@Entity(name="Facet_EvernoteNotebook")
@ObjectTypeSpec(name = "notebook", value = 2, prettyname = "Notebook")
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
