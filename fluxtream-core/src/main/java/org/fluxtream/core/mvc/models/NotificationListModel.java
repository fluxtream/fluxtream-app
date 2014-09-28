package org.fluxtream.core.mvc.models;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.core.domain.Notification;

/**
 * Created by justin on 3/26/14.
 */
public class NotificationListModel {
    public List<NotificationModel> notifications = null;

    public NotificationListModel(){

    }

    public NotificationListModel(List<Notification> notifications){
        for (Notification n : notifications)
            addNotification(n);
    }

    public void addNotification(NotificationModel nm){
        if (notifications == null)
            notifications = new ArrayList<NotificationModel>();
        notifications.add(nm);
    }

    public void addNotification(Notification n){
        addNotification(new NotificationModel(n));
    }



}
