package org.fluxtream.core.api;

import com.google.gson.Gson;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.DashboardWidget;
import org.fluxtream.core.services.WidgetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/v1/widgets")
@Component("RESTWidgetCollection")
@Scope("request")
public class WidgetCollection {

    @Autowired
    WidgetsService widgetsService;
    
    Gson gson = new Gson();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableWidgetsList() {
        try{
            long guestId = AuthHelper.getGuestId();
            List<DashboardWidget> widgets = widgetsService.getAvailableWidgetsList(guestId);
            return Response.ok(gson.toJson(widgets)).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to get available widgets: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/refresh")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response refreshWidgets() {
        try{
            long guestId = AuthHelper.getGuestId();
            widgetsService.refreshWidgets(guestId);
            return Response.ok("widgets refreshed").build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to regresh widgets: " + e.getMessage()).build();
        }
    }

}
