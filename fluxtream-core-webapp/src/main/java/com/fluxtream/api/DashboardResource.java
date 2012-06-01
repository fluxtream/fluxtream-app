package com.fluxtream.api;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.domain.Dashboard;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.DashboardService;
import com.google.gson.Gson;
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
        return gson.toJson(dashboards);
    }

    @POST
    @Path("/dashboard")
    @Produces({ MediaType.APPLICATION_JSON })
    public String addDashboard(String dashboardName) {
        long guestId = ControllerHelper.getGuestId();
        return null;
    }

    @DELETE
    @Path("/dashboard")
    @Produces({ MediaType.APPLICATION_JSON })
    public String removeDashboard(String dashboardName) {
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
    public String setDashboardsOrder(@QueryParam("dashboardName") String dashboardNames) {
        long guestId = ControllerHelper.getGuestId();
        return null;
    }


}
