package org.fluxtream.connectors.google_calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * User: candide
 * Date: 27/07/13
 * Time: 12:31
 */
public class GoogleCalendarConnectorSettings implements Serializable {

    public List<CalendarConfig> calendars = new ArrayList<CalendarConfig>();

    void addCalendarConfig(CalendarConfig config){
        calendars.add(config);
    }

    CalendarConfig getCalendar(String id) {
        for (CalendarConfig calendar : calendars) {
            if (calendar.id.equals(id))
                return calendar;
        }
        return null;
    }

}
