package org.fluxtream.core.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Entity;

/**
 * User: candide
 * Date: 30/09/14
 * Time: 14:30
 */
@Entity(name="SystemInfo")
public class SystemInfo extends AbstractEntity  {

    @Type(type="yes_no")
    public boolean channelMappingsFixupWasExecuted;

    public SystemInfo() {}

}
