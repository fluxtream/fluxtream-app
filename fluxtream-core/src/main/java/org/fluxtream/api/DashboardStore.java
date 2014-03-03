package org.fluxtream.api;

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
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.domain.Dashboard;
import org.fluxtream.domain.DashboardWidget;
import org.fluxtream.domain.WidgetSettings;
import org.fluxtream.mvc.models.StatusModel;
import org.fluxtream.services.DashboardsService;
import org.fluxtream.services.WidgetsService;
import org.fluxtream.utils.Utils;
import com.google.gson.Gson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.fluxtream.aspects.FlxLogger;
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

    FlxLogger logger = FlxLogger.getLogger(DashboardStore.class);

    @Autowired
    DashboardsService dashboardsService;

    @Autowired
    WidgetsService widgetsService;

    Gson gson = new Gson();

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDashboards() {
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
            return jsonArray.toString();
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get dashboards: " + e.getMessage()));
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
    public String addDashboard(@FormParam("dashboardName") String dashboardName) throws UnsupportedEncodingException {
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
            return gson.toJson(new StatusModel(false,"Failed to add dashboard: " + e.getMessage()));
        }
    }

    @DELETE
    @Path("/{dashboardId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String removeDashboard(@PathParam("dashboardId") long dashboardId) throws UnsupportedEncodingException {
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
            return gson.toJson(new StatusModel(false,"Failed to delete dashboard: " + e.getMessage()));
        }
    }

    @GET
    @Path("/{dashboardId}/availableWidgets")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAvailableWidgets(@PathParam("dashboardId") long dashboardId) {
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
            return gson.toJson(widgetsNotYetInDashboard);
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=getAvailableWidgets")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to get availableWidgets: " + e.getMessage()));
        }
    }

    @PUT
    @Path("/{dashboardId}/name")
    @Produces({ MediaType.APPLICATION_JSON })
    public String renameDashboard(@PathParam("dashboardId") long dashboardId,
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
            return gson.toJson(new StatusModel(false,"Failed to rename dashboard: " + e.getMessage()));
        }
    }

    @PUT
    @Path("/{dashboardId}/active")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setActiveDashboard(@PathParam("dashboardId") long dashboardId)
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
            return gson.toJson(new StatusModel(false,"Failed to set active dashboard: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{dashboardId}/widgets")
    @Produces({ MediaType.APPLICATION_JSON })
    public String addWidget(@PathParam("dashboardId") long dashboardId,
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
            return gson.toJson(new StatusModel(false,"Failed to add widget: " + e.getMessage()));
        }
    }

    @DELETE
    @Path("/{dashboardId}/widgets/{widgetName}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String removeWidget(@PathParam("dashboardId") long dashboardId,
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
            return gson.toJson(new StatusModel(false,"failed to remove widget: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{dashboardId}/widgets/reorder")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setWidgetsOrder(@PathParam("dashboardId") long dashboardId,
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
            return gson.toJson(new StatusModel(false,"Failed to set widget order: " + e.getMessage()));
        }
    }

    @POST
    @Path("/reorder")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setDashboardsOrder(@FormParam("dashboardIds") String dashboardIds) throws UnsupportedEncodingException {
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
            return gson.toJson(new StatusModel(false,"Failed to set dashboard order: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{dashboardId}/widgets/{widgetName}/settings")
    @Produces({ MediaType.APPLICATION_JSON })
    public String saveWidgetSettings(@PathParam("dashboardId") long dashboardId,
                                     @PathParam("widgetName") String widgetName,
                                     @FormParam("settingsJSON") String settingsJSON) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            widgetName = URLDecoder.decode(widgetName, "UTF-8");
            widgetsService.saveWidgetSettings(guestId, dashboardId, widgetName, settingsJSON);
            StatusModel statusModel = new StatusModel(true, "Successfully saved widget settings");
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=saveWidgetSettings")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return gson.toJson(statusModel);
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=saveWidgetSettings")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to save widget settings: " + e.getMessage()));
        }
    }

    @GET
    @Path("/{dashboardId}/widgets/{widgetName}/settings")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getWidgetSettings(@PathParam("dashboardId") long dashboardId,
                                    @PathParam("widgetName") String widgetName) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try{
            widgetName = URLDecoder.decode(widgetName, "UTF-8");
            final WidgetSettings settings = widgetsService.getWidgetSettings(guestId, dashboardId, widgetName);
            JSONObject jsonSettings = JSONObject.fromObject(settings.settingsJSON);
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=getWidgetSettings")
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return jsonSettings.toString();
        }
        catch (Exception e){
            StringBuilder sb = new StringBuilder("module=API component=dashboardStore action=getWidgetSettings")
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to get widget settings: " + e.getMessage()));
        }
    }

}
