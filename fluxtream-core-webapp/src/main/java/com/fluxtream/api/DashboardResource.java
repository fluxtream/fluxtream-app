package com.fluxtream.api;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.domain.Dashboard;
import com.fluxtream.domain.DashboardWidget;
import com.fluxtream.domain.DashboardWidgetsRepository;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.DashboardService;
import com.google.gson.Gson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Candide Kemmler (candide@fluxteam.com)
 */
@Path("/dashboards")
@Component("dashboardsApi")
@Scope("request")
public class DashboardResource {

    @Autowired
    DashboardService dashboardService;

    Gson gson = new Gson();

    @GET
    @Path("/all")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDashboards() {
        long guestId = ControllerHelper.getGuestId();
        List<Dashboard> dashboards = dashboardService.getDashboards(guestId);
        Collections.sort(dashboards);
        JSONArray jsonArray = new JSONArray();
        for (final ListIterator eachDashboard = dashboards.listIterator(); eachDashboard.hasNext(); ) {
            final Dashboard dashboard = (Dashboard)eachDashboard.next();
            JSONObject dashboardJson = new JSONObject();
            dashboardJson.accumulate("name", dashboard.name);
            JSONObject widgetsJson = JSONObject.fromObject(dashboard.widgetsJson);
            dashboardJson.accumulate("widgets", widgetsJson);
            jsonArray.add(dashboardJson);
        }
        return jsonArray.toString();
    }

    @POST
    @Path("/dashboard")
    @Produces({ MediaType.APPLICATION_JSON })
    public String addDashboard(@QueryParam("dashboardName") String dashboardName) {
        long guestId = ControllerHelper.getGuestId();

        return null;
    }

    @DELETE
    @Path("/dashboard")
    @Produces({ MediaType.APPLICATION_JSON })
    public String removeDashboard(@QueryParam("dashboardName") String dashboardName) {
        long guestId = ControllerHelper.getGuestId();
        return null;
    }
    @PUT
    @Path("/dashboard/name")
    @Produces({ MediaType.APPLICATION_JSON })
    public String renameDashboard(@QueryParam("previousName") String previousName,
                                  @QueryParam("dashboardName") String dashboardName) {
        long guestId = ControllerHelper.getGuestId();
        return null;
    }

    @POST
    @Path("/dashboard/widget")
    @Produces({ MediaType.APPLICATION_JSON })
    public String addWidget(@QueryParam("dashboardName") String dashboardName,
                            @QueryParam("widget") String widgetJson) {
        long guestId = ControllerHelper.getGuestId();
        return null;
    }

    @DELETE
    @Path("/dashboard/widget")
    @Produces({ MediaType.APPLICATION_JSON })
    public String removeWidget(@QueryParam("dashboardName") String dashboardName,
                               @QueryParam("widgetName") String widgetName) {
        long guestId = ControllerHelper.getGuestId();
        return null;
    }

    @PUT
    @Path("/dashboard/widgets/order")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setWidgetsOrder(@QueryParam("dashboardName") String dashboardName,
                                  @QueryParam("widgetNames") String widgetNames) {
        long guestId = ControllerHelper.getGuestId();
        return null;
    }

    @PUT
    @Path("/order")
    @Produces({ MediaType.APPLICATION_JSON })
    public String setDashboardsOrder(@QueryParam("dashboardNames") String dashboardNames) {
        long guestId = ControllerHelper.getGuestId();
        return null;
    }

    @GET
    @Path("/widgets")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAvailableWidgetsList() {
        long guestId = ControllerHelper.getGuestId();
        List<DashboardWidget> widgets = dashboardService.getAvailableWidgetsList(guestId);
        return gson.toJson(widgets);
    }

    @GET
    @Path("/widgets/refresh")
    @Produces({ MediaType.APPLICATION_JSON })
    public String refreshWidgets() {
        long guestId = ControllerHelper.getGuestId();
        dashboardService.refreshWidgets(guestId);
        return gson.toJson(new StatusModel(true, "widgets refreshed"));
    }

    @GET
    @Path("/repositories")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getWidgetRepositories() {
        long guestId = ControllerHelper.getGuestId();
        final List<DashboardWidgetsRepository> repositories = dashboardService.getWidgetRepositories(guestId);
        JSONArray result = new JSONArray();
        for (DashboardWidgetsRepository repository : repositories)
            result.add(repository.url);
        return result.toString();
    }

    @PUT
    @Path("/repositories/repository")
    @Produces({ MediaType.APPLICATION_JSON })
    public String addWidgetRepositoryURL(@QueryParam("url") String url) {
        long guestId = ControllerHelper.getGuestId();
        dashboardService.addWidgetRepositoryURL(guestId, url);
        return gson.toJson(new StatusModel(true, "added widget repository"));
    }

    @DELETE
    @Path("/repositories/repository")
    @Produces({ MediaType.APPLICATION_JSON })
    public String removeWidgetRepositoryURL(@QueryParam("url") String url) {
        long guestId = ControllerHelper.getGuestId();
        dashboardService.removeWidgetRepositoryURL(guestId, url);
        return gson.toJson(new StatusModel(true, "removed widget repository"));
    }

}
