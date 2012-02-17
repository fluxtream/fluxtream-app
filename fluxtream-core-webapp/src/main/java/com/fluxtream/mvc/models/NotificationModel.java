package com.fluxtream.mvc.models;

import com.fluxtream.domain.Notification;

public class NotificationModel {

	String message;
	String type;
	long id;
	
	public NotificationModel(Notification notification) {
		this.message = notification.message;
		this.type = notification.type.name().toLowerCase();
		this.id = notification.getId();
	}
	
}
