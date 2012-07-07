package com.fluxtream.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.domain.Dashboard;
import com.fluxtream.domain.DashboardWidget;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.DashboardsService;
import com.fluxtream.services.WidgetsService;
import com.google.gson.Gson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.velocity.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Candide Kemmler (candide@fluxteam.com)
 */
@Path("/dashboards")
@Component("RESTDashboardStore")
@Scope("request")
public class DashboardStore {

    @Autowired
    DashboardsService dashboardsService;

    @Autowired
    WidgetsService widgetsService;

    Gson gson = new Gson();

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDashboards() {
        long guestId = ControllerHelper.getGuestId();
        List<Dashboard> dashboards = dashboardsService.getDashboards(guestId);
        Collections.sort(dashboards);
        JSONArray jsonArray = new JSONArray();
        for (final ListIterator eachDashboard = dashboards.listIterator(); eachDashboard.hasNext(); ) {
            final Dashboard dashboard = (Dashboard)eachDashboard.next();
            JSONObject dashboardJson = toDashboardJson(dashboard, guestId);
            jsonArray.add(dashboardJson);
        }
        return jsonArray.toString();
    }

    private JSONObject toDashboardJson(final Dashboard dashboard, long guestId) {
        JSONObject dashboardJson = new JSONObject();
        dashboardJson.accumulate("id", dashboard.getId());
        dashboardJson.accumulate("name", dashboard.name);
        dashboardJson.accumulate("active", dashboard.active);
        final List<DashboardWidget> availableWidgetsList = widgetsService.getAvailableWidgetsList(guestId);
        final String[] widgetNames = StringUtils.split(dashboard.widgetNames, ",");
        JSONArray widgetsArray = new JSONArray();
        there: for (String widgetName : widgetNames) {
            for (DashboardWidget dashboardWidget : availableWidgetsList) {
                if (dashboardWidget.WidgetName.equals(widgetName)) {
                    widgetsArray.add(toJSONObject(dashboardWidget));
                    continue there;
                }
            }
        }
        dashboardJson.accumulate("widgets", widgetsArray);
        return dashboardJson;
    }

    private JSONObject toJSONObject(final DashboardWidget dashboardWidget) {
        JSONObject widgetJson = new JSONObject();
        widgetJson.accumulate("WidgetName", dashboardWidget.WidgetName);
        widgetJson.accumulate("WidgetRepositoryURL", dashboardWidget.WidgetRepositoryURL);
        widgetJson.accumulate("WidgetDescription", dashboardWidget.WidgetDescription);
        widgetJson.accumulate("WidgetTitle", dashboardWidget.WidgetTitle);
        widgetJson.accumulate("WidgetIcon", dashboardWidget.WidgetIcon);
        return widgetJson;
    }

    private JSONArray toJsonArray(final String commaSeparatedWidgetNames) {
        final String[] widgetNames = StringUtils.split(commaSeparatedWidgetNames, ",");
        JSONArray names = new JSONArray();
        for (String name : widgetNames)
            names.add(name);
        return names;
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    public String addDashboard(@FormParam("dashboardName") String dashboardName) throws UnsupportedEncodingException {
        long guestId = ControllerHelper.getGuestId();
        dashboardsService.addDashboard(guestId, dashboardName);
        return getDashboards();
    }

    @DELETE
    @Path("/{dashboardId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String removeDashboard(@PathParam("dashboardId") long dashboardId) throws UnsupportedEncodingException {
        long guestId = ControllerHelper.getGuestId();
        dashboardsService.removeDashboard(guestId, dashboardId);
        return getDashboards();
    }

    @GET
    @Path("/{dashboardId}/availableWidgets")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAvailableWidgets(@PathParam("dashboardId") long dashboardId) {
        long guestId = ControllerHelper.getGuestId();
        final List<DashboardWidget> availableWidgetsList = widgetsService.getAvailableWidgetsList(guestId);
        final List<DashboardWidget> widgetsNotYetInDashboard = new ArrayList<DashboardWidget>();
        final Dashboard dashboard = dashboardsService.getDashboard(guestId, dashboardId);
        final String[] dashboardWidgets = StringUtils.split(dashboard.widgetNames, ",");
        outerloop: for (DashboardWidget availableWidget : availableWidgetsList) {
            for (String dashboardWidgetName : dashboardWidgets) {
                if (availableWidget.WidgetName.equals(dashboardWidgetName))
                    continue outerloop;
            }
            widgetsNotYetInDashboard.add(availableWidget);
        }
        return gson.toJson(widgetsNotYetInDashboard);
    }

    @PUT
    @Path("/{dashboardId}/name")
    @Produces({ MediaType.APPLICATION_JSON })
    public String renameDashboard(@PathParam("dashboardId") long dashboardId,
                                  @QueryParam("name") String newName) throws UnsupportedEncodingException {
        newName = URLDecoder.decode(newName, "UTF-8");
        long guestId = ControllerHelper.getGuestId();
        dashboardsService.renameDashboard(guestId, dashboardId, newName);
        return getDashboards();
    }

    @PUT
    @Path("/{dashboardId}/active")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setActiveDashboard(@PathParam("dashboardId") long dashboardId)
            throws UnsupportedEncodingException {
        long guestId = ControllerHelper.getGuestId();
        dashboardsService.setActiveDashboard(guestId, dashboardId);
        return getDashboards();
    }

    @POST
    @Path("/{dashboardId}/widgets")
    @Produces({ MediaType.APPLICATION_JSON })
    public String addWidget(@PathParam("dashboardId") long dashboardId,
                            @FormParam("widget") String widgetJson) throws UnsupportedEncodingException {
        widgetJson = URLDecoder.decode(widgetJson, "UTF-8");
        long guestId = ControllerHelper.getGuestId();
        dashboardsService.addWidget(guestId, dashboardId, widgetJson);
        return getDashboards();
    }

    @DELETE
    @Path("/{dashboardId}/widgets/{widgetName}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String removeWidget(@PathParam("dashboardId") long dashboardId,
                               @PathParam("widgetName") String widgetName) throws UnsupportedEncodingException {
        widgetName = URLDecoder.decode(widgetName, "UTF-8");
        long guestId = ControllerHelper.getGuestId();
        dashboardsService.removeWidget(guestId, dashboardId, widgetName);
        return getDashboards();
    }

    @POST
    @Path("/{dashboardId}/widgets/reorder")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setWidgetsOrder(@PathParam("dashboardId") long dashboardId,
                                  @FormParam("widgetNames") String widgetNames) throws UnsupportedEncodingException {
        long guestId = ControllerHelper.getGuestId();
        final String[] wNames = StringUtils.split(widgetNames, ",");
        dashboardsService.setWidgetsOrder(guestId, dashboardId, wNames);
        return getDashboards();
    }

    @POST
    @Path("/reorder")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setDashboardsOrder(@FormParam("dashboardIds") String dashboardIds) throws UnsupportedEncodingException {
        //dashboardIds = URLDecoder.decode(dashboardIds, "UTF-8");
        long guestId = ControllerHelper.getGuestId();
        final String[] dNames = StringUtils.split(dashboardIds, ",");
        long[] ids = new long[dNames.length];
        int i=0;
        for (String dName : dNames) {
            ids[i++] = Long.valueOf(dName);
        }
        dashboardsService.setDashboardsOrder(guestId, ids);
        return getDashboards();
    }

}
