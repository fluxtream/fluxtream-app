package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Created by candide on 06/12/14.
 */
@ApiModel(description="Available channels and wether they are shared with the requested user")
public class SharedChannelModel {


    @ApiModelProperty(required=true)
    public long channelId;

    @ApiModelProperty(required=true)
    public String deviceName;
    @ApiModelProperty(required=true)
    public String channelName;

    @ApiModelProperty(required=true)
    public boolean shared;

    @ApiModelProperty(required=true)
    public boolean userData;

}
