package org.fluxtream.api;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.fluxtream.Configuration;
import org.fluxtream.aspects.FlxLogger;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.domain.Guest;
import org.fluxtream.mvc.models.CalendarModel;
import org.fluxtream.mvc.models.StatusModel;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.MetadataService;
import org.fluxtream.utils.TimeUtils;
import com.google.gson.Gson;
import org.joda.time.LocalDate;
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
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=getModel")
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value = "/setToToday")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setToToday(@QueryParam("timeUnit") String timeUnit,
                             @QueryParam("state") String state) throws IOException {
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setToToday")
                .append(" guestId=").append(guestId)
                .append(" state=").append(state);
        logger.info(sb.toString());
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value = "/getWeek")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setWeek(@QueryParam("state") String state,
                          @QueryParam("year") int year,
                          @QueryParam("week") int week)
            throws IOException {
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=getWeek")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        calendarModel.setWeek(year, week);
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value="/getDateRangeForWeek")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDateRangeForWeek(@QueryParam("week") int week,
                                      @QueryParam("year") int year) {
        final LocalDate firstOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        final String startDate = TimeUtils.dateFormatter.print(firstOfWeek);
        final String endDate = TimeUtils.dateFormatter.print(firstOfWeek.plusWeeks(1).minusDays(1));

        return String.format("[\"%s\", \"%s\"]", startDate, endDate);
    }

    @GET
    @Path(value="/getMeTheJavaComputedWeekForThisDate")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDateWeek(@QueryParam("formattedDate") String formattedDate) {
        final String[] splits = formattedDate.split("-");
        final int year = Integer.valueOf(splits[0]);
        final int month = Integer.valueOf(splits[1]);
        final int date = Integer.valueOf(splits[2]);

        final LocalDate curr = new LocalDate(year, month, date);

        // Need to adjust forward one week if the day of the week is a Sunday - this is
        // the first day of the next week in CalendarModel, but is the last day of the
        // current week in LocalDate
        final LocalDate firstOfWeek;
        if (curr.getDayOfWeek() < TimeUtils.FIRST_DAY_OF_WEEK)
            firstOfWeek = curr.withDayOfWeek(TimeUtils.FIRST_DAY_OF_WEEK);
        else
            firstOfWeek = curr.plusWeeks(1).withDayOfWeek(TimeUtils.FIRST_DAY_OF_WEEK);

        return String.format("[%d, %d]",
                             firstOfWeek.getWeekyear(),
                             firstOfWeek.getWeekOfWeekyear());
    }

    @GET
    @Path(value = "/getMonth")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setMonth(@QueryParam("state") String state,
                           @QueryParam("year") int year,
                           @QueryParam("month") int month)
            throws IOException {
        logger.info("action=setMonth year=" + year + " month=" + month);
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=getMonth")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        final CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        calendarModel.setMonth(year, month);
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value = "/getYear")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setYear(@QueryParam("state") String state,
                          @QueryParam("year") int year)
            throws IOException {
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=getYear")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        calendarModel.setYear(year);
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value = "/getDate")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setDate(@QueryParam("date") String date)
            throws IOException {
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setDate")
                .append(" guestId=").append(guestId)
                .append(" date=").append(date);
        logger.info(sb.toString());
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        calendarModel.setDate(date);
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value = "/decrementTimespan")
    @Produces({ MediaType.APPLICATION_JSON })
    public String decrementTimespan(@QueryParam("state") String state) throws IOException {
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=decrementTimespan")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.decrementTimespan();
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value = "/incrementTimespan")
    @Produces({ MediaType.APPLICATION_JSON })
    public String incrementTimespan(@QueryParam("state") String state) throws IOException {
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=incrementTimespan")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.incrementTimespan();
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value = "/setDayTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setDayTimeUnit(@QueryParam("state") String state) throws IOException {
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setDayTimeUnit")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.setDayTimeUnit();
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value = "/setWeekTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setWeekTimeUnit(@QueryParam("state") String state) throws IOException {
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setWeekTimeUnit")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.setWeekTimeUnit();
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value = "/setMonthTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setMonthTimeUnit(@QueryParam("state") String state) throws IOException {
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setMonthTimeUnit")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.setMonthTimeUnit();
        return calendarModel.toJSONString(env);
    }

    @GET
    @Path(value = "/setYearTimeUnit")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setYearTimeUnit(@QueryParam("state") String state) throws IOException {
        long guestId;
        try {
            Guest guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setYearTimeUnit")
                .append(" state=").append(state)
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, state);
        calendarModel.setYearTimeUnit();
        return calendarModel.toJSONString(env);
    }
}
