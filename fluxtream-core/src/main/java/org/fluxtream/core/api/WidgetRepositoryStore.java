package org.fluxtream.core.api;

import com.google.gson.Gson;
import net.sf.json.JSONArray;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.DashboardWidgetsRepository;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.WidgetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.fluxtream.core.api.RESTUtils.handleRuntimeException;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/repositories")
@Component("RESTWidgetRepositoryStore")
@Scope("request")
public class WidgetRepositoryStore {

    @Autowired
    WidgetsService widgetsService;

    Gson gson = new Gson();

    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getWidgetRepositories() {
        try{
            long guestId = AuthHelper.getGuestId();
            final List<DashboardWidgetsRepository> repositories = widgetsService.getWidgetRepositories(guestId);
            JSONArray result = new JSONArray();
            for (DashboardWidgetsRepository repository : repositories) {
                result.add(repository.url);
            }
            return result.toString();
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get widget repositories: " + e.getMessage()));
        }
    }

    @POST
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    public String addWidgetRepositoryURL(@FormParam("url") String url) {
        try{
            long guestId = AuthHelper.getGuestId();
            try { widgetsService.addWidgetRepositoryURL(guestId, url); }
            catch (RuntimeException rte) { return handleRuntimeException(rte); }
            return gson.toJson(new StatusModel(true, "added widget repository"));
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to add widget repository: " + e.getMessage()));
        }
    }

    @DELETE
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/")
    public String removeWidgetRepositoryURL(@QueryParam("url") String url) {
        try{
            long guestId = AuthHelper.getGuestId();
            widgetsService.removeWidgetRepositoryURL(guestId, url);
            return gson.toJson(new StatusModel(true, "removed widget repository"));
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to delete widget repository: " + e.getMessage()));
        }
    }
}
