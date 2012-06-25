package com.fluxtream.services.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.fluxtream.domain.Notification;
import com.fluxtream.domain.Notification.Type;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.utils.JPAUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
@Component
public class NotificationsServiceImpl implements NotificationsService {

	@PersistenceContext
	EntityManager em;

	@Override
	@Transactional(readOnly = false)
	public void addNotification(long guestId, Type type, String message) {
		Notification notification = new Notification();
		notification.guestId = guestId;
		notification.type = type;
		notification.message = message;
		em.persist(notification);
	}

	@Override
	@Transactional(readOnly = false)
	public void deleteNotification(long guestId, long notificationId) {
		Notification notification = em.find(Notification.class, notificationId);
		if (notification.guestId!=guestId) {
			throw new RuntimeException("attempt to delete a notification from the wrong guest");
		}
		notification.deleted = true;
		em.merge(notification);
	}

	@Override
	public List<Notification> getNotifications(long guestId) {
		List<Notification> notifications = JPAUtils.find(em, Notification.class, "notifications.all", guestId);
		return notifications;
	}

}
