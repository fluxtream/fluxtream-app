package org.fluxtream.core.services;

import org.fluxtream.core.domain.Event;
import org.fluxtream.core.events.EventListener;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface EventListenerService {

    public <T extends Event> void addEventListener(Class<T> eventClass, EventListener<T> listener);

    public void fireEvent(Event event);

}
