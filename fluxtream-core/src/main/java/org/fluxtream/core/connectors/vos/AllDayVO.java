package org.fluxtream.core.connectors.vos;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * User: candide
 * Date: 01/11/14
 * Time: 09:41
 */
public interface AllDayVO {

    @JsonProperty
    public boolean allDay();

}
