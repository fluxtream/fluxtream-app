package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.fluxtream.core.domain.Notification;
import org.joda.time.format.ISODateTimeFormat;

@ApiModel(value = "Background Update notification")
public class NotificationModel {

    @ApiModelProperty(value="Message as displayed to the end-user", required=true)
    public String message;

    @ApiModelProperty(value="Message as displayed to the end-user", required=true, allowableValues = "WARNING, ERROR, INFO")
    public String type;

    @ApiModelProperty(value="UTC time in ISO8601 format", required=true)
    public String time;

    @ApiModelProperty(value="The notification's unique id", required=true)
    public long id;

    @ApiModelProperty(value="Number of times this notification has already been issued", required=true)
    public int repeated;

	public NotificationModel(Notification notification) {
		this.message = notification.message;
		this.type = notification.type.name().toLowerCase();
		this.id = notification.getId();
        this.repeated = notification.repeated;
        this.time = ISODateTimeFormat.dateHourMinuteSecond().withZoneUTC().print(notification.ts)+"Z";
    }

}
