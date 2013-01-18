package com.fluxtream.connectors.runkeeper;

import javax.persistence.Embeddable;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */

@Embeddable
public class HeartRateMeasure {

    public double heartRate;
    public double timestamp;

}
