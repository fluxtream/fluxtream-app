package org.fluxtream.mvc.models;

import org.fluxtream.domain.Notification;
import org.joda.time.format.ISODateTimeFormat;

public class NotificationModel {

	String message;
	String type;
    String time;
	long id;
    int repeated;

	public NotificationModel(Notification notification) {
		this.message = notification.message;
		this.type = notification.type.name().toLowerCase();
		this.id = notification.getId();
        this.repeated = notification.repeated;
        this.time = ISODateTimeFormat.dateHourMinuteSecond().withZoneUTC().print(notification.ts)+"Z";
    }

}
