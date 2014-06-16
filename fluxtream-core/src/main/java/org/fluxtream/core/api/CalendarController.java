package org.fluxtream.core.api;

import com.google.gson.Gson;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.mvc.models.CalendarModel;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */

@Path("/v1/calendar/nav")
@Component("RESTCalendarController")
@Scope("request")
public class CalendarController {

    FlxLogger logger = FlxLogger.getLogger(CalendarController.class);

    @Autowired
    GuestService guestService;

    @Autowired
    MetadataService metadataService;

    Gson gson = new Gson();

    @Autowired
    Configuration env;

    @GET
    @Path(value = "/model")
    @Produces({ MediaType.APPLICATION_JSON } )
    public String getModel(@QueryParam("state") String state) throws IOException {
        long guestId;
        Guest guest = AuthHelper.getGuest();
        guestId = guest.getId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=getModel")
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        return calendarModel.toJSONString(env);
    }

}
