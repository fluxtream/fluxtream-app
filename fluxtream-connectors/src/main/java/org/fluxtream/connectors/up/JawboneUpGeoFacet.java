package org.fluxtream.connectors.up;

import javax.persistence.MappedSuperclass;
import org.fluxtream.connectors.Connector;

/**
 * User: candide
 * Date: 07/02/14
 * Time: 14:13
 */
@MappedSuperclass
public abstract class JawboneUpGeoFacet extends JawboneUpFacet {

    public Double place_lat;
    public Double place_lon;
    public Integer place_acc;
    public String place_name;

    public JawboneUpGeoFacet() {
        this.api = Connector.getConnector("up").value();
    }

    public JawboneUpGeoFacet(long apiKeyId) {
        super(apiKeyId);
        this.api = Connector.getConnector("up").value();
    }
}
