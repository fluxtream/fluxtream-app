package org.fluxtream.core.api;

import com.google.gson.Gson;
import net.sf.json.JSONArray;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.DashboardWidgetsRepository;
import org.fluxtream.core.services.WidgetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/v1/repositories")
@Component("RESTWidgetRepositoryStore")
@Scope("request")
public class WidgetRepositoryStore {

    @Autowired
    WidgetsService widgetsService;

    Gson gson = new Gson();

    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getWidgetRepositories() {
        try{
            long guestId = AuthHelper.getGuestId();
            final List<DashboardWidgetsRepository> repositories = widgetsService.getWidgetRepositories(guestId);
            JSONArray result = new JSONArray();
            for (DashboardWidgetsRepository repository : repositories) {
                result.add(repository.url);
            }
            return Response.ok(result.toString()).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to get widget repositories: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addWidgetRepositoryURL(@FormParam("url") String url) {
        try{
            long guestId = AuthHelper.getGuestId();
            widgetsService.addWidgetRepositoryURL(guestId, url);
            return Response.ok("added widget repository").build();
        }
        catch (Throwable e){
            return Response.serverError().entity("Failed to add widget repository: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/")
    public Response removeWidgetRepositoryURL(@QueryParam("url") String url) {
        try{
            long guestId = AuthHelper.getGuestId();
            widgetsService.removeWidgetRepositoryURL(guestId, url);
            return Response.ok("removed widget repository").build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to delete widget repository: " + e.getMessage()).build();
        }
    }
}
