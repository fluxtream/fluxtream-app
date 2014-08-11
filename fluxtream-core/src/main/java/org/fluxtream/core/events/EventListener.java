package org.fluxtream.core.events;

import org.fluxtream.core.domain.Event;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface EventListener<T extends Event> {

    public void handleEvent(final T event);

}