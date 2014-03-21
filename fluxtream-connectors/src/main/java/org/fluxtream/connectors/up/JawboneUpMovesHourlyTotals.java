package org.fluxtream.connectors.up;

import javax.persistence.Embeddable;

/**
 * User: candide
 * Date: 29/01/14
 * Time: 15:12
 */
@Embeddable
public class JawboneUpMovesHourlyTotals {

    public long start;

    public Integer distance;
    public Double calories;
    public Integer steps;
    public Integer active_time;
    public Integer inactive_time;
    public Integer longest_active_time;
    public Integer longest_idle_time;

    public JawboneUpMovesHourlyTotals() {}

}
