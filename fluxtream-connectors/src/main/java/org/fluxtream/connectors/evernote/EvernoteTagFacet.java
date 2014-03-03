package org.fluxtream.connectors.evernote;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 05/12/13
 * Time: 16:43
 */
@Entity(name="Facet_EvernoteTag")
@ObjectTypeSpec(name = "tag", value = 8, prettyname = "Tag", clientFacet = false)
@NamedQueries({
      @NamedQuery(name = "evernote.tags.byApiKeyId", query = "SELECT facet FROM Facet_EvernoteTag facet WHERE facet.apiKeyId=?")
})
public class EvernoteTagFacet extends EvernoteFacet {

    public String name;

    public EvernoteTagFacet() { super(); }

    public EvernoteTagFacet(long apiKeyId) {super(apiKeyId);}

    @Override
    protected void makeFullTextIndexable() {
        fullTextDescription = name;
    }
}
