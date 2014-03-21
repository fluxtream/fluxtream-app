package org.fluxtream.services.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.fluxtream.domain.Notification;
import org.fluxtream.domain.Notification.Type;
import org.fluxtream.services.NotificationsService;
import org.fluxtream.utils.JPAUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Component
@Transactional(readOnly=true)
public class NotificationsServiceImpl implements NotificationsService {

	@PersistenceContext
	EntityManager em;

    @Override
    @Transactional(readOnly = false)
    public void addNamedNotification(final long guestId, final Type type, final String name, String message) {
        final Notification previousNotification = JPAUtils.findUnique(em,
                                                                      Notification.class,
                                                                      "notifications.withName",
                                                                      guestId, name);
        if (previousNotification==null) {
            Notification notification = new Notification();
            notification.guestId = guestId;
            notification.type = type;
            notification.message = message;
            notification.name = name;
            em.persist(notification);
        } else {
            previousNotification.deleted = false;
            previousNotification.type = type;
            previousNotification.message = message;
            previousNotification.ts = System.currentTimeMillis();
            em.merge(previousNotification);
        }
    }

    @Override
	@Transactional(readOnly = false)
	public void addNotification(long guestId, Type type, String message) {
        final Notification sameNotification = JPAUtils.findUnique(em,
              Notification.class,
              "notifications.withTypeAndMessage",
              guestId, type, message);
        if (sameNotification==null) {
            Notification notification = new Notification();
            notification.guestId = guestId;
            notification.type = type;
            notification.message = message;
            em.persist(notification);
        } else {
            sameNotification.ts = System.currentTimeMillis();
            sameNotification.repeated++;
            em.merge(sameNotification);
        }
	}

    @Override
    public void addExceptionNotification(final long guestId, final Type type, final String message, final String stackTrace) {
        final Notification sameNotification = JPAUtils.findUnique(em,
                                                                  Notification.class,
                                                                  "notifications.withTypeAndMessage",
                                                                  guestId, type, message);
        if (sameNotification==null) {
            Notification notification = new Notification();
            notification.guestId = guestId;
            notification.type = type;
            notification.message = message;
            notification.stackTrace = stackTrace;
            notification.ts = System.currentTimeMillis();
            em.persist(notification);
        } else {
            sameNotification.stackTrace = stackTrace;
            sameNotification.repeated++;
            sameNotification.ts = System.currentTimeMillis();
            em.merge(sameNotification);
        }
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

    @Override
    public Notification getNamedNotification(final long guestId, final String name) {
        final Notification notification = JPAUtils.findUnique(em,
                                                                      Notification.class,
                                                                      "notifications.withName",
                                                                      guestId, name);
        return notification;
    }

}
