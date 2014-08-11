package org.fluxtream.core.services.impl;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.fluxtream.core.domain.Event;
import org.fluxtream.core.events.EventListener;
import org.fluxtream.core.services.EventListenerService;
import org.fluxtream.core.aspects.FlxLogger;
import org.codehaus.plexus.util.ExceptionUtils;
import org.springframework.stereotype.Service;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Service
public class EventListenerServiceImpl implements EventListenerService {

    private final FlxLogger logger = FlxLogger.getLogger(EventListenerServiceImpl.class);

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
        StringBuilder msgAtts = new StringBuilder("module=events component=EventListenerServiceImpl action=fireEvent");
        try {
            StringBuilder sb = new StringBuilder(msgAtts);
            if (event!=null) sb.append(" event=").append(event.toString());
            logger.info(sb.toString());
            List<EventListener> eventListeners = listeners.get(event.getClass().getName());
            if (eventListeners!=null) {
                for (EventListener eventListener : eventListeners) {
                    eventListener.handleEvent(event);
                }
            } else {
                logger.info(new StringBuffer(msgAtts)
                                    .append(" message=\"No Event Listeners were registered for events of type ")
                                    .append(event.getClass())
                                    .append("\"").toString());
            }
        } catch (Throwable t) {
            logger.warn(new StringBuffer(msgAtts)
                                .append(" message=\"Error firing event\"")
                                .append(" stackTrace=<![CDATA[" + ExceptionUtils.getStackTrace(t))
                                .append("]]>").toString());
        }
    }

}
