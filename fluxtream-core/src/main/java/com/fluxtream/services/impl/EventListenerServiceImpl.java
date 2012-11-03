package com.fluxtream.services.impl;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import com.fluxtream.domain.Event;
import com.fluxtream.events.EventListener;
import com.fluxtream.services.EventListenerService;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Service
public class EventListenerServiceImpl implements EventListenerService {

    private final Logger logger = Logger.getLogger(EventListenerServiceImpl.class);

    Map<String, List<EventListener>> listeners =
            new Hashtable<String,List<EventListener>>();

    @Override
    public <T extends Event> void addEventListener(final Class<T> eventClass, final EventListener<T> listener) {
        StringBuilder sb = new StringBuilder("module=events component=EventListenerServiceImpl action=addEventListener");
        if (eventClass!=null)
            sb.append(" eventClass=" + eventClass.toString());
        if (listener!=null)
            sb.append(" listener=" + listener.toString());
        logger.info(sb.toString());
        if (listeners.get(eventClass.getName())==null) {
            listeners.put(eventClass.getName(), new Vector<EventListener>());
        }
        sb = new StringBuilder("module=events component=EventListenerServiceImpl action=addEventListener");
        if (!listeners.get(eventClass.getName()).contains(listener)) {
            logger.info(sb.append(" message=\"adding listener " + listener.toString() + "\"").toString());
            listeners.get(eventClass.getName()).add(listener);
        } else {
            logger.warn(sb.append(" message=\"preventing duplicate listener registration\"").toString());
        }
    }

    @Override
    public void fireEvent(final Event event) {
        StringBuilder sb = new StringBuilder("module=events component=EventListenerServiceImpl action=fireEvent");
        if (event!=null) sb.append(" event=").append(event.toString());
        logger.info(sb.toString());
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners!=null) {
            for (EventListener eventListener : eventListeners) {
                eventListener.handleEvent(event);
            }
        } else {
            logger.warn("No Event Listeners were registered for events of type " + event.getClass());
        }
    }

}
