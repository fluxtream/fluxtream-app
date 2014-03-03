package org.fluxtream.connectors.up;

import javax.persistence.MappedSuperclass;
import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.AbstractFacet;
import org.hibernate.annotations.Index;

/**
 * User: candide
 * Date: 07/02/14
 * Time: 14:13
 */
@MappedSuperclass
public abstract class JawboneUpFacet extends AbstractFacet {

    @Index(name="xid")
    public String xid;
    public Long time_created;
    public Long time_updated;
    public Long time_completed;

    @Index(name="date")
    public String date;
    public String tz;

    public JawboneUpFacet() {
        this.api = Connector.getConnector("up").value();
    }

    public JawboneUpFacet(long apiKeyId) {
        super(apiKeyId);
        this.api = Connector.getConnector("up").value();
    }
}
