package com.fluxtream.api;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/updates")
@Component("RESTUpdateStore")
@Scope("request")
public class UpdateStore {

    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    GuestService guestService;

    @GET
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getUpdates(@PathParam("connector") String connectorName,
                             @QueryParam("pageSize") int pageSize,
                             @QueryParam("page") int page) {
        try{
            long guestId = AuthHelper.getGuestId();
            final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
            final List<ApiUpdate> updates = connectorUpdateService.getUpdates(apiKey, pageSize, page);
            return gson.toJson(updates);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get updates: " + e.getMessage()));
        }
    }

}
