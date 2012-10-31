package com.fluxtream.events;

import com.fluxtream.domain.Event;
import com.fluxtream.events.push.PushEvent;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface EventListener<T extends Event> {

    public void handleEvent(final T event);

}