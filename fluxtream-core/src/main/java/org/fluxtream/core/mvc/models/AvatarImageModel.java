package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * User: candide
 * Date: 08/07/14
 * Time: 11:53
 */
@ApiModel(value = "Generic data model for CalendarData Store operations")
public class AvatarImageModel {

    @ApiModelProperty(value="The type of avatar", allowableValues = "facebook, gravatar", required=true)
    public String type;
    @ApiModelProperty(value="The gravatar image's URL", required=true)
    public String url;

    public AvatarImageModel() {}

}
