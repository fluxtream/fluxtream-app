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

import static com.newrelic.api.agent.NewRelic.setTransactionName;

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
        setTransactionName(null, "GET /calendar/nav/model");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=getModel")
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @POST
    @Path(value = "/setToToday")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setToToday(@QueryParam("timeUnit") String timeUnit,
                             @QueryParam("state") String state) throws IOException {
        setTransactionName(null, "POST /calendar/nav/setToToday");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setToToday")
                .append(" guestId=").append(guestId)
                .append(" state=").append(state);
        logger.info(sb.toString());
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @GET
    @Path(value = "/getWeek")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setWeek(@QueryParam("state") String state,
                          @QueryParam("year") int year,
                          @QueryParam("week") int week)
            throws IOException {
        setTransactionName(null, "GET /calendar/nav/getWeek");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=getWeek")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        calendarModel.setWeek(guestId, metadataService, year, week);
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @GET
    @Path(value = "/getMonth")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setMonth(@QueryParam("state") String state,
                           @QueryParam("year") int year,
                           @QueryParam("month") int month)
            throws IOException {
        setTransactionName(null, "GET /calendar/nav/getMonth");
        logger.info("action=setMonth year=" + year + " month=" + month);
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=getMonth")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        calendarModel.setMonth(guestId, metadataService, year, month);
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @GET
    @Path(value = "/getYear")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setYear(@QueryParam("state") String state,
                          @QueryParam("year") int year)
            throws IOException {
        setTransactionName(null, "GET /calendar/nav/getYear");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=getYear")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        calendarModel.setYear(guestId, metadataService, year);
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @GET
    @Path(value = "/getDate")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setDate(@QueryParam("date") String date)
            throws IOException {
        setTransactionName(null, "GET /calendar/nav/getDate");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setDate")
                .append(" guestId=").append(guestId)
                .append(" date=").append(date);
        logger.info(sb.toString());
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        calendarModel.setDate(guestId, metadataService, date);
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @POST
    @Path(value = "/decrementTimespan")
    @Produces({ MediaType.APPLICATION_JSON })
    public String decrementTimespan(@QueryParam("state") String state) throws IOException {
        setTransactionName(null, "POST /calendar/nav/decrementTimespan");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=decrementTimespan")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.decrementTimespan(guestId, metadataService, state);
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @POST
    @Path(value = "/incrementTimespan")
    @Produces({ MediaType.APPLICATION_JSON })
    public String incrementTimespan(@QueryParam("state") String state) throws IOException {
        setTransactionName(null, "POST /calendar/nav/incrementTimespan");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=incrementTimespan")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.incrementTimespan(guestId, metadataService, state);
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @POST
    @Path(value = "/setDayTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setDayTimeUnit(@QueryParam("state") String state) throws IOException {
        setTransactionName(null, "POST /calendar/nav/setDayTimeUnit");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setDayTimeUnit")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.setDayTimeUnit();
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @POST
    @Path(value = "/setWeekTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setWeekTimeUnit(@QueryParam("state") String state) throws IOException {
        setTransactionName(null, "POST /calendar/nav/setWeekTimeUnit");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setWeekTimeUnit")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.setWeekTimeUnit();
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @POST
    @Path(value = "/setMonthTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setMonthTimeUnit(@QueryParam("state") String state) throws IOException {
        setTransactionName(null, "POST /calendar/nav/setMonthTimeUnit");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setMonthTimeUnit")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.setMonthTimeUnit();
        return calendarModel.toJSONString(guestId, metadataService, env);
    }

    @POST
    @Path(value = "/setYearTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setYearTimeUnit(@QueryParam("state") String state) throws IOException {
        setTransactionName(null, "POST /calendar/nav/setYearTimeUnit");
        final long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setYearTimeUnit")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.setYearTimeUnit();
        return calendarModel.toJSONString(guestId, metadataService, env);
    }


}
