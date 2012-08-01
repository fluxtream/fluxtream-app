package com.fluxtream.api;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.Configuration;
import com.fluxtream.TimeUnit;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.CalendarModel;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */

@Path("/calendar/nav")
@Component("RESTCalendarController")
@Scope("request")
public class CalendarController {

    Logger logger = Logger.getLogger(CalendarController.class);

    @Autowired
    GuestService guestService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    Configuration env;

    @GET
    @Path(value = "/model")
    @Produces({ MediaType.APPLICATION_JSON } )
    public String getModel(@QueryParam("state") String state) throws IOException {
        CalendarModel calendarModel = CalendarModel.fromState(ControllerHelper.getGuestId(), metadataService, state);
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @POST
    @Path(value = "/setToToday")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setToToday(@QueryParam("timeUnit") String timeUnit,
                             @QueryParam("state") String state) throws IOException {
        logger.info("action=setToToday");
        CalendarModel calendarModel = new CalendarModel(ControllerHelper.getGuestId(), metadataService);
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @GET
    @Path(value = "/getWeek")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setWeek(@QueryParam("state") String state,
                          @QueryParam("year") int year,
                          @QueryParam("week") int week)
            throws IOException {
        logger.info("action=setWeek year=" + year + " week=" + week);
        CalendarModel calendarModel = new CalendarModel(ControllerHelper.getGuestId(), metadataService);
        calendarModel.setWeek(ControllerHelper.getGuestId(), metadataService, year, week);
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @GET
    @Path(value = "/getMonth")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setMonth(@QueryParam("state") String state,
                           @QueryParam("year") int year,
                           @QueryParam("month") int month)
            throws IOException {
        logger.info("action=setMonth year=" + year + " month=" + month);
        CalendarModel calendarModel = new CalendarModel(ControllerHelper.getGuestId(), metadataService);
        calendarModel.setMonth(ControllerHelper.getGuestId(), metadataService, year, month);
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @GET
    @Path(value = "/getYear")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setYear(@QueryParam("state") String state,
                          @QueryParam("year") int year)
            throws IOException {
        logger.info("action=setYear year=" + year);
        CalendarModel calendarModel = new CalendarModel(ControllerHelper.getGuestId(), metadataService);
        calendarModel.setYear(ControllerHelper.getGuestId(), metadataService, year);
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @GET
    @Path(value = "/getDate")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setDate(@QueryParam("date") String date)
            throws IOException {
        logger.info("action=setDate date=" + date);
        CalendarModel calendarModel = new CalendarModel(ControllerHelper.getGuestId(), metadataService);
        calendarModel.setDate(ControllerHelper.getGuestId(), metadataService, date);
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @POST
    @Path(value = "/decrementTimespan")
    @Produces({ MediaType.APPLICATION_JSON })
    public String decrementTimespan(@QueryParam("state") String state) throws IOException {
        CalendarModel calendarModel = CalendarModel.fromState(ControllerHelper.getGuestId(), metadataService, state);
        calendarModel.decrementTimespan(ControllerHelper.getGuestId(), metadataService, state);
        logger.info("action=decrementTimespan");
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @POST
    @Path(value = "/incrementTimespan")
    @Produces({ MediaType.APPLICATION_JSON })
    public String incrementTimespan(@QueryParam("state") String state) throws IOException {
        CalendarModel calendarModel = CalendarModel.fromState(ControllerHelper.getGuestId(), metadataService, state);
        calendarModel.incrementTimespan(ControllerHelper.getGuestId(), metadataService, state);
        logger.info("action=incrementTimespan");
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @POST
    @Path(value = "/setDayTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setDayTimeUnit(@QueryParam("state") String state) throws IOException {
        logger.info("action=setDayTimeUnit");
        CalendarModel calendarModel = CalendarModel.fromState(ControllerHelper.getGuestId(), metadataService, state);
        calendarModel.setDayTimeUnit();
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @POST
    @Path(value = "/setWeekTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setWeekTimeUnit(@QueryParam("state") String state) throws IOException {
        CalendarModel calendarModel = CalendarModel.fromState(ControllerHelper.getGuestId(), metadataService, state);
        calendarModel.setWeekTimeUnit();
        logger.info("action=setWeekTimeUnit");
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @POST
    @Path(value = "/setMonthTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setMonthTimeUnit(@QueryParam("state") String state) throws IOException {
        CalendarModel calendarModel = CalendarModel.fromState(ControllerHelper.getGuestId(), metadataService, state);
        calendarModel.setMonthTimeUnit();
        logger.info("action=setMonthTimeUnit");
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }

    @POST
    @Path(value = "/setYearTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setYearTimeUnit(@QueryParam("state") String state) throws IOException {
        CalendarModel calendarModel = CalendarModel.fromState(ControllerHelper.getGuestId(), metadataService, state);
        calendarModel.setYearTimeUnit();
        logger.info("action=setYearTimeUnit");
        return calendarModel.toJSONString(ControllerHelper.getGuestId(), metadataService, env);
    }


}
