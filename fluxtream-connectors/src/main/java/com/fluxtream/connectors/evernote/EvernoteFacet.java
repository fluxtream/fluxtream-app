package com.fluxtream.connectors.evernote;

import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.AbstractFacet;

/**
 * User: candide
 * Date: 05/12/13
 * Time: 16:45
 */
public abstract class EvernoteFacet extends AbstractFacet {

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
