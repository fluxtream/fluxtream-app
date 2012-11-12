package com.fluxtream.events;

import com.fluxtream.domain.Event;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface EventListener<T extends Event> {

    public void handleEvent(final T event);

}