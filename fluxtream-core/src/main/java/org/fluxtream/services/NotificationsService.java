package org.fluxtream.services;

import java.util.List;

import org.fluxtream.domain.Notification;

public interface NotificationsService {

    public void addNamedNotification(long guestId, Notification.Type type, String name, String message);

	public void addNotification(long guestId, Notification.Type type, String message);

    public void addExceptionNotification(long guestId, Notification.Type type, String message, String stackTrace);

	public void deleteNotification(long guestId, long notificationId);
	
	public List<Notification> getNotifications(long guestId);

    public Notification getNamedNotification(final long guestId, final String name);
	
}
