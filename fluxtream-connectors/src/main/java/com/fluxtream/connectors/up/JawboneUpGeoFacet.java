package com.fluxtream.connectors.up;

import javax.persistence.MappedSuperclass;
import com.fluxtream.connectors.Connector;

/**
 * User: candide
 * Date: 07/02/14
 * Time: 14:13
 */
@MappedSuperclass
public abstract class JawboneUpGeoFacet extends JawboneUpFacet {

    public double place_lat;
    public double place_lon;
    public int place_acc;
    public String place_name;

    public JawboneUpGeoFacet() {
        this.api = Connector.getConnector("up").value();
    }

    public JawboneUpGeoFacet(long apiKeyId) {
        super(apiKeyId);
        this.api = Connector.getConnector("up").value();
    }
}
