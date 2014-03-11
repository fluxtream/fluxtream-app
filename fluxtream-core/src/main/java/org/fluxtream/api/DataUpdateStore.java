package org.fluxtream.api;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.domain.DataUpdate;
import org.fluxtream.mvc.models.DataUpdateDigestModel;
import org.fluxtream.mvc.models.StatusModel;
import org.fluxtream.services.DataUpdateService;
import org.fluxtream.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/dataUpdates")
@Component("DataUpdateStore")
@Scope("request")
public class DataUpdateStore {

    Gson gson = new GsonBuilder().create();

    @Autowired
    GuestService guestService;

    @Autowired
    DataUpdateService dataUpdateService;

    @GET
    @Path("/all")
    @Produces({MediaType.APPLICATION_JSON})
    public String getDataUpdates(@QueryParam("since") long since){
        try{
            List<DataUpdate> updates = dataUpdateService.getAllUpdatesSince(AuthHelper.getGuestId(),since);
            return gson.toJson(new DataUpdateDigestModel(updates));
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to fetch updates"));
        }
    }
}
