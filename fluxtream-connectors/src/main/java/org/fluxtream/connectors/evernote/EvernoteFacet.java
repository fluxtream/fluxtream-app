package org.fluxtream.connectors.evernote;

import javax.persistence.MappedSuperclass;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.AbstractFacet;
import org.hibernate.annotations.Index;

/**
 * User: candide
 * Date: 05/12/13
 * Time: 16:45
 */
@MappedSuperclass
public abstract class EvernoteFacet extends AbstractFacet {

    @Index(name="guid")
    public String guid;
    public Integer USN;

    public EvernoteFacet() {
        this.api = Connector.getConnector("evernote").value();
    }

    public EvernoteFacet(long apiKeyId) {
        super(apiKeyId);
        this.api = Connector.getConnector("evernote").value();
    }

    @Override
    protected void makeFullTextIndexable() {
    }

}
