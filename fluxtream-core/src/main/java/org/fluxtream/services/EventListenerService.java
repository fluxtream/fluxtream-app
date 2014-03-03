package org.fluxtream.services;

import org.fluxtream.domain.Event;
import org.fluxtream.events.EventListener;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface EventListenerService {

    public <T extends Event> void addEventListener(Class<T> eventClass, EventListener<T> listener);

    public void fireEvent(Event event);

}
