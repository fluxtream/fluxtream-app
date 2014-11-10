package org.fluxtream.core.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import net.sf.json.JSONObject;
import org.apache.velocity.util.StringUtils;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Dashboard;
import org.fluxtream.core.domain.DashboardWidget;
import org.fluxtream.core.domain.WidgetSettings;
import org.fluxtream.core.mvc.models.DashboardModel;
import org.fluxtream.core.mvc.models.DashboardWidgetManifestModel;
import org.fluxtream.core.mvc.models.DashboardWidgetModel;
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
@Api(value = "/dashboards", description = "Widget dashboards API")
@Scope("request")
    public class DashboardStore {

    FlxLogger logger = FlxLogger.getLogger(DashboardStore.class);

    @Autowired
    DashboardsService dashboardsService;

    @Autowired
    WidgetsService widgetsService;

    @GET
    @ApiOperation(value = "Get a user's dashboards", response = DashboardModel.class, responseContainer = "Array")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getDashboards() {
        long guestId = AuthHelper.getGuestId();
        try {
            List<Dashboard> dashboards = dashboardsService.getDashboards(guestId);
            Collections.sort(dashboards);
            List<DashboardModel> dashboardModels = new ArrayList<DashboardModel>();
            for (final ListIterator eachDashboard = dashboards.listIterator(); eachDashboard.hasNext(); ) {
                final Dashboard dashboard = (Dashboard)eachDashboard.next();
                DashboardModel dashboardModel = toDashboardModel(dashboard, guestId);
                dashboardModels.add(dashboardModel);
            }
            return Response.ok(dashboardModels).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to get dashboards: " + e.getMessage()).build();
        }
    }

    private DashboardModel toDashboardModel(final Dashboard dashboard, long guestId) {
        DashboardModel dashboardModel = new DashboardModel();
        dashboardModel.id = dashboard.getId();
        dashboardModel.name = dashboard.name;
        dashboardModel.active = dashboard.active;
        final List<DashboardWidget> availableWidgetsList = widgetsService.getAvailableWidgetsList(guestId);
        final String[] widgetNames = StringUtils.split(dashboard.widgetNames, ",");
        List<DashboardWidgetModel> widgetModels = new ArrayList<DashboardWidgetModel>();
        there: for (String widgetName : widgetNames) {
            for (DashboardWidget dashboardWidget : availableWidgetsList) {
                if (dashboardWidget.WidgetName.equals(widgetName)) {
                    widgetModels.add(toDashboardWidgetModel(dashboardWidget, dashboard, guestId));
                    continue there;
                }
            }
        }
        dashboardModel.widgets = widgetModels;
        return dashboardModel;
    }

    private DashboardWidgetModel toDashboardWidgetModel(final DashboardWidget dashboardWidget,
                                                        final Dashboard dashboard,
                                                        final Long guestId) {
        DashboardWidgetManifestModel widgetManifestModel = new DashboardWidgetManifestModel();
        widgetManifestModel.WidgetName = dashboardWidget.WidgetName;
        widgetManifestModel.WidgetRepositoryURL = dashboardWidget.WidgetRepositoryURL;
        widgetManifestModel.WidgetDescription = dashboardWidget.WidgetDescription;
        widgetManifestModel.WidgetTitle = dashboardWidget.WidgetTitle;
        widgetManifestModel.WidgetIcon = dashboardWidget.WidgetIcon;
        widgetManifestModel.HasSettings = dashboardWidget.HasSettings;

        DashboardWidgetModel widgetModel = new DashboardWidgetModel();
        widgetModel.manifest = widgetManifestModel;

        if (dashboard!=null && dashboardWidget.HasSettings) {
            final WidgetSettings widgetSettings = widgetsService.getWidgetSettings(guestId, dashboard.getId(),
                    dashboardWidget.WidgetName);
            widgetModel.settings = widgetSettings.settingsJSON;
        }

        return widgetModel;
    }

    @POST
    @ApiOperation(value = "Add a dashboard", response = DashboardModel.class, responseContainer = "Array")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addDashboard(@ApiParam(value="The dashboard's name", required=true) @FormParam("dashboardName") String dashboardName) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            dashboardsService.addDashboard(guestId, dashboardName);
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
    @ApiOperation(value = "Delete a dashboard", response = DashboardModel.class, responseContainer = "Array")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response removeDashboard(@PathParam("dashboardId") long dashboardId) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            dashboardsService.removeDashboard(guestId, dashboardId);
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
    @ApiOperation(value = "List available widgets", response = DashboardWidget.class, responseContainer = "Array")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getAvailableWidgets(@PathParam("dashboardId") long dashboardId) {
        long guestId = AuthHelper.getGuestId();
        try {
            final List<DashboardWidget> widgetsNotYetInDashboard = new ArrayList<DashboardWidget>();
            final List<DashboardWidget> availableWidgetsList = widgetsService.getAvailableWidgetsList(guestId);
            final Dashboard dashboard = dashboardsService.getDashboard(guestId, dashboardId);
            final String[] dashboardWidgets = StringUtils.split(dashboard.widgetNames, ",");
            outerloop: for (DashboardWidget availableWidget : availableWidgetsList) {
                for (String dashboardWidgetName : dashboardWidgets) {
                    if (availableWidget.WidgetName.equals(dashboardWidgetName))
                        continue outerloop;
                }
                widgetsNotYetInDashboard.add(availableWidget);
            }
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
    @ApiOperation(value = "Rename a dashboard", response = DashboardModel.class, responseContainer = "Array")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response renameDashboard(@PathParam("dashboardId") long dashboardId,
                                    @QueryParam("name") String newName) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            newName = URLDecoder.decode(newName, "UTF-8");
            dashboardsService.renameDashboard(guestId, dashboardId, newName);
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
    @ApiOperation(value = "Set current/active dashboard", response = DashboardModel.class, responseContainer = "Array")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response setActiveDashboard(@PathParam("dashboardId") long dashboardId)
            throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            dashboardsService.setActiveDashboard(guestId, dashboardId);
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
    @ApiOperation(value = "Add a widget to a dashboard", response = DashboardModel.class, responseContainer = "Array")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addWidget(@PathParam("dashboardId") long dashboardId,
                              @FormParam("widget") String widgetJson) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            widgetJson = URLDecoder.decode(widgetJson, "UTF-8");
            dashboardsService.addWidget(guestId, dashboardId, widgetJson);
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
    @ApiOperation(value = "Remove a widget from a dashboard", response = DashboardModel.class, responseContainer = "Array")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response removeWidget(@PathParam("dashboardId") long dashboardId,
                                 @PathParam("widgetName") String widgetName) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            widgetName = URLDecoder.decode(widgetName, "UTF-8");
            dashboardsService.removeWidget(guestId, dashboardId, widgetName);
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
    @ApiOperation(value = "Change the order of widgets in a dashboard", response = DashboardModel.class, responseContainer = "Array")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response setWidgetsOrder(@PathParam("dashboardId") long dashboardId,
                                    @FormParam("widgetNames") String widgetNames) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            final String[] wNames = StringUtils.split(widgetNames, ",");
            dashboardsService.setWidgetsOrder(guestId, dashboardId, wNames);
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
    @ApiOperation(value = "Change the order of dashboards", response = DashboardModel.class, responseContainer = "Array")
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
    @ApiOperation(value = "Save a widget's settings")
    public Response saveWidgetSettings(@PathParam("dashboardId") long dashboardId,
                                       @PathParam("widgetName") String widgetName,
                                       @FormParam("settingsJSON") String settingsJSON) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try {
            widgetName = URLDecoder.decode(widgetName, "UTF-8");
            widgetsService.saveWidgetSettings(guestId, dashboardId, widgetName, settingsJSON);
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
    @ApiOperation(value = "Get a widget's settings", response = DashboardModel.class, responseContainer = "Array")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getWidgetSettings(@PathParam("dashboardId") long dashboardId,
                                      @PathParam("widgetName") String widgetName) throws UnsupportedEncodingException {
        long guestId = AuthHelper.getGuestId();
        try{
            widgetName = URLDecoder.decode(widgetName, "UTF-8");
            final WidgetSettings settings = widgetsService.getWidgetSettings(guestId, dashboardId, widgetName);
            JSONObject jsonSettings = JSONObject.fromObject(settings.settingsJSON);
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
