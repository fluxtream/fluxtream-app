package com.fluxtream.services.impl;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import com.fluxtream.domain.Event;
import com.fluxtream.events.EventListener;
import com.fluxtream.services.EventListenerService;
import org.springframework.stereotype.Service;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Service
public class EventListenerServiceImpl implements EventListenerService {

    Map<String, List<EventListener>> listeners =
            new Hashtable<String,List<EventListener>>();

    @Override
    public <T extends Event> void addEventListener(final Class<T> eventClass, final EventListener<T> listener) {
        final List<EventListener> eventListeners = listeners.get(eventClass.getName());
        if (eventListeners==null)
            listeners.put(eventClass.getName(), new Vector<EventListener>());
        eventListeners.add(listener);
    }

    @Override
    public void fireEvent(final Event event) {
        List<EventListener> eventListeners = listeners.get(event.getClass());
        for (EventListener eventListener : eventListeners) {
            eventListener.handleEvent(event);
        }
    }

}
