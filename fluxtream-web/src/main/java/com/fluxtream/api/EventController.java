package com.fluxtream.api;

import java.io.IOException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.events.push.PushEvent;
import com.fluxtream.services.EventListenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This controller serves no real purpose but to test the Event Framework
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/events")
@Component("RESTTestEventController")
@Scope("request")
public class EventController {

    @Autowired
    EventListenerService eventListenerService;

    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    @Path("/push/{connectorName}/{eventType}")
    public String testPushEvent(@PathParam("connectorName") String connectorName,
                                @PathParam("eventType") String eventType,
                                @QueryParam("flxGuestId") long flxGuestId) throws IOException {
        PushEvent pushEvent = new PushEvent(flxGuestId, connectorName, eventType, null);
        eventListenerService.fireEvent(pushEvent);
        return "event fired";
    }

}
