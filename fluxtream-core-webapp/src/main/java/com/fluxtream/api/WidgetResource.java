package com.fluxtream.api;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.domain.DashboardWidget;
import com.fluxtream.domain.DashboardWidgetsRepository;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.WidgetsService;
import com.google.gson.Gson;
import net.sf.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.fluxtream.api.RESTUtils.handleRuntimeException;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/widgets")
@Component("widgetsApi")
@Scope("request")
public class WidgetResource {

    @Autowired
    WidgetsService widgetsService;
    
    Gson gson = new Gson();

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAvailableWidgetsList() {
        long guestId = ControllerHelper.getGuestId();
        try {
            List<DashboardWidget> widgets = widgetsService.getAvailableWidgetsList(guestId);
            return gson.toJson(widgets);
        } catch (RuntimeException rte) {
            return handleRuntimeException(rte);
        }
    }

    @POST
    @Path("/refresh")
    @Produces({ MediaType.APPLICATION_JSON })
    public String refreshWidgets() {
        long guestId = ControllerHelper.getGuestId();
        widgetsService.refreshWidgets(guestId);
        return gson.toJson(new StatusModel(true, "widgets refreshed"));
    }

}
