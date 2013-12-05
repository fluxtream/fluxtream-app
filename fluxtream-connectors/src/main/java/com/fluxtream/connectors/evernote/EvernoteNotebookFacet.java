package com.fluxtream.connectors.evernote;

import javax.persistence.Entity;

/**
 * User: candide
 * Date: 05/12/13
 * Time: 17:00
 */
@Entity(name="Facet_EvernoteNotebook")
public class EvernoteNotebookFacet extends EvernoteFacet {

    public EvernoteNotebookFacet() { super(); }

    public EvernoteNotebookFacet(long apiKeyId) {super(apiKeyId);}

    @Override
    protected void makeFullTextIndexable() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
