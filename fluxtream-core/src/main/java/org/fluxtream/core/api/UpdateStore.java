package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ApiUpdate;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/v1/updates")
@Component("RESTUpdateStore")
@Scope("request")
public class UpdateStore {

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();

    @Autowired
    GuestService guestService;

    public static List<String> nonRepeatableUpdateQueriesConnectors =
            Arrays.asList("fitbit", "google_calendar", "withings", "bodymedia", "up");

    public static class ApiUpdatesModel {
        @Expose
        public boolean repeatable;
        @Expose
        public String connectorName;
        @Expose
        public List<ApiUpdate> updates;
        public ApiUpdatesModel(final String connectorName, final List<ApiUpdate> updates, final boolean repeatable) {
            this.connectorName = connectorName;
            this.updates = updates;
            this.repeatable = repeatable;
        }
    }

    @GET
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUpdates(@PathParam("connector") String connectorName,
                               @QueryParam("pageSize") int pageSize,
                               @QueryParam("page") int page) {
        try {
            long guestId = AuthHelper.getGuestId();
            final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
            final List<ApiUpdate> updates = connectorUpdateService.getUpdates(apiKey, pageSize, page);
            final ApiUpdatesModel updatesModel = new ApiUpdatesModel(connectorName,
                    updates, !nonRepeatableUpdateQueriesConnectors.contains(connectorName));
            return Response.ok(gson.toJson(updatesModel)).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to get updates: " + e.getMessage()).build();
        }
    }

}
