package com.fluxtream.services;

import java.util.List;

import com.fluxtream.domain.Notification;

public interface NotificationsService {

	public void addNotification(long guestId, Notification.Type type, String message);

    public void addNotification(long guestId, Notification.Type type, String message,
                                String stackTrace);

	public void deleteNotification(long guestId, long notificationId);
	
	public List<Notification> getNotifications(long guestId);
	
}
