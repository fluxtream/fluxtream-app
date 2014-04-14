package org.fluxtream.core.api;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.DashboardWidget;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.WidgetsService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/widgets")
@Component("RESTWidgetCollection")
@Scope("request")
public class WidgetCollection {

    @Autowired
    WidgetsService widgetsService;
    
    Gson gson = new Gson();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getAvailableWidgetsList() {
        try{
            long guestId = AuthHelper.getGuestId();
            List<DashboardWidget> widgets = widgetsService.getAvailableWidgetsList(guestId);
            return gson.toJson(widgets);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get available widgets: " + e.getMessage()));
        }
    }

    @POST
    @Path("/refresh")
    @Produces({ MediaType.APPLICATION_JSON })
    public String refreshWidgets() {
        try{
            long guestId = AuthHelper.getGuestId();
            widgetsService.refreshWidgets(guestId);
            return gson.toJson(new StatusModel(true, "widgets refreshed"));
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to regresh widgets: " + e.getMessage()));
        }
    }

}
