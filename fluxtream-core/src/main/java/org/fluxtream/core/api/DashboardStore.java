package org.fluxtream.core.api;

import com.google.gson.Gson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.velocity.util.StringUtils;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Dashboard;
import org.fluxtream.core.domain.DashboardWidget;
import org.fluxtream.core.domain.WidgetSettings;
import org.fluxtream.core.services.DashboardsService;
import org.fluxtream.core.services.WidgetsService;
import org.fluxtream.core.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Candide Kemmler (candide@fluxteam.com)
 */
@Path("/v1/dashboards")
@Component("RESTDashboardStore")
@Scope("request")
    public class DashboardStore {

    FlxLogger logger = FlxLogger.getLogger(DashboardStore.class);

    @Autowired
    DashboardsService dashboardsService;

    @Autowired
    WidgetsService widgetsService;

    Gson gson = new Gson();

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getDashboards() {
        long guestId = AuthHelper.getGuestId();
        try {
            List<Dashboard> dashboards = dashboardsService.getDashboards(guestId);
            Collections.sort(dashboards);
            JSONArray jsonArray = new JSONArray();
            for (final ListIterator eachDashboard = dashboards.listIterator(); eachDashboard.hasNext(); ) {
                final Dashboard dashboard = (Dashboard)eachDashboard.next();
                JSONObject dashboardJson = toDashboardJson(dashboard, guestId);
                jsonArray.add(dashboardJson);
            }
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=getDashboards")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return Response.ok(jsonArray.toString()).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to get dashboards: " + e.getMessage()).build();
        }
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
                    widgetsArray.add(toJSONObject(guestId, dashboard, dashboardWidget));
                    continue there;
                }
            }
        }
        dashboardJson.accumulate("widgets", widgetsArray);
        return dashboardJson;
    }

    private JSONObject toJSONObject(final long guestId, final Dashboard dashboard,
                                    final DashboardWidget dashboardWidget) {
        JSONObject manifestJSON = new JSONObject();
        manifestJSON.accumulate("WidgetName", dashboardWidget.WidgetName);
        manifestJSON.accumulate("WidgetRepositoryURL", dashboardWidget.WidgetRepositoryURL);
        manifestJSON.accumulate("WidgetDescription", dashboardWidget.WidgetDescription);
        manifestJSON.accumulate("WidgetTitle", dashboardWidget.WidgetTitle);
        manifestJSON.accumulate("WidgetIcon", dashboardWidget.WidgetIcon);
        manifestJSON.accumulate("HasSettings", dashboardWidget.HasSettings);

        JSONObject widgetJSON = new JSONObject();
        widgetJSON.accumulate("manifest", manifestJSON);

        if (dashboardWidget.HasSettings) {
            final WidgetSettings widgetSettings = widgetsService.getWidgetSettings(guestId, dashboard.getId(),
                                                                                   dashboardWidget.WidgetName);
            final JSONObject jsonSettings = JSONObject.fromObject(widgetSettings.settingsJSON);
            widgetJSON.accumulate("settings", jsonSettings);
        }

        return widgetJSON;
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
    public Response addDashboard(@FormParam("dashboardName") String dashboardName) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            dashboardsService.addDashboard(guestId, dashboardName);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=addDashboard")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return getDashboards();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=addDashboard")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to add dashboard: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{dashboardId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response removeDashboard(@PathParam("dashboardId") long dashboardId) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            dashboardsService.removeDashboard(guestId, dashboardId);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=deleteDashboard")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return getDashboards();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=deleteDashboard")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to delete dashboard: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/{dashboardId}/availableWidgets")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getAvailableWidgets(@PathParam("dashboardId") long dashboardId) {
        long guestId = AuthHelper.getGuestId();
        try {
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
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=getAvailableWidgets")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return Response.ok(widgetsNotYetInDashboard).build();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=getAvailableWidgets")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to get availableWidgets: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{dashboardId}/name")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response renameDashboard(@PathParam("dashboardId") long dashboardId,
                                  @QueryParam("name") String newName) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            newName = URLDecoder.decode(newName, "UTF-8");
            dashboardsService.renameDashboard(guestId, dashboardId, newName);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=renameDashboard")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return getDashboards();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=renameDashboard")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to rename dashboard: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{dashboardId}/active")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response setActiveDashboard(@PathParam("dashboardId") long dashboardId)
            throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            dashboardsService.setActiveDashboard(guestId, dashboardId);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=setActiveDashboard")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return getDashboards();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=setActiveDashboard")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to set active dashboard: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/{dashboardId}/widgets")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addWidget(@PathParam("dashboardId") long dashboardId,
                            @FormParam("widget") String widgetJson) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            widgetJson = URLDecoder.decode(widgetJson, "UTF-8");
            dashboardsService.addWidget(guestId, dashboardId, widgetJson);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=addWidget")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return getDashboards();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=addWidget")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to add widget: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{dashboardId}/widgets/{widgetName}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response removeWidget(@PathParam("dashboardId") long dashboardId,
                               @PathParam("widgetName") String widgetName) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            widgetName = URLDecoder.decode(widgetName, "UTF-8");
            dashboardsService.removeWidget(guestId, dashboardId, widgetName);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=removeWidget")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return getDashboards();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=removeWidget")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to remove widget: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/{dashboardId}/widgets/reorder")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response setWidgetsOrder(@PathParam("dashboardId") long dashboardId,
                                  @FormParam("widgetNames") String widgetNames) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            final String[] wNames = StringUtils.split(widgetNames, ",");
            dashboardsService.setWidgetsOrder(guestId, dashboardId, wNames);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=setWidgetOrder")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return getDashboards();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=setWidgetOrder")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to set widget order: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/reorder")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response setDashboardsOrder(@FormParam("dashboardIds") String dashboardIds) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            //dashboardIds = URLDecoder.decode(dashboardIds, "UTF-8");
            final String[] dNames = StringUtils.split(dashboardIds, ",");
            long[] ids = new long[dNames.length];
            int i=0;
            for (String dName : dNames) {
                ids[i++] = Long.valueOf(dName);
            }
            dashboardsService.setDashboardsOrder(guestId, ids);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=setDashboardsOrder")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return getDashboards();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=setDashboardsOrder")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to set dashboard order: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/{dashboardId}/widgets/{widgetName}/settings")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response saveWidgetSettings(@PathParam("dashboardId") long dashboardId,
                                     @PathParam("widgetName") String widgetName,
                                     @FormParam("settingsJSON") String settingsJSON) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            widgetName = URLDecoder.decode(widgetName, "UTF-8");
            widgetsService.saveWidgetSettings(guestId, dashboardId, widgetName, settingsJSON);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=saveWidgetSettings")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return Response.ok("Successfully saved widget settings").build();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=saveWidgetSettings")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to save widget settings: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/{dashboardId}/widgets/{widgetName}/settings")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getWidgetSettings(@PathParam("dashboardId") long dashboardId,
                                    @PathParam("widgetName") String widgetName) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try{
            widgetName = URLDecoder.decode(widgetName, "UTF-8");
            final WidgetSettings settings = widgetsService.getWidgetSettings(guestId, dashboardId, widgetName);
            JSONObject jsonSettings = JSONObject.fromObject(settings.settingsJSON);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=getWidgetSettings")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return Response.ok(jsonSettings.toString()).build();
        }
        catch (Exception e){
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=getWidgetSettings")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return Response.serverError().entity("Failed to get widget settings: " + e.getMessage()).build();
        }
    }

}
