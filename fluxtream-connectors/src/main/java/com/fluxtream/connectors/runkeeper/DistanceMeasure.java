package com.fluxtream.connectors.runkeeper;

import javax.persistence.Embeddable;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Embeddable
public class DistanceMeasure {

    public double distance;
    public double timestamp;

}
