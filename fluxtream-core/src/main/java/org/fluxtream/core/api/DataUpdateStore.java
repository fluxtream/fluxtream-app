package org.fluxtream.core.api;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.DataUpdate;
import org.fluxtream.core.mvc.models.DataUpdateDigestModel;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.DataUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.SettingsService;
import org.joda.time.format.ISODateTimeFormat;
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
    SettingsService settingsService;

    @Autowired
    DataUpdateService dataUpdateService;

    @GET
    @Path("/all")
    @Produces({MediaType.APPLICATION_JSON})
    public String getDataUpdates(@QueryParam("since") String since){
        try{
            List<DataUpdate> updates = dataUpdateService.getAllUpdatesSince(AuthHelper.getGuestId(), ISODateTimeFormat.basicDateTime().parseMillis(since));
            return gson.toJson(new DataUpdateDigestModel(updates,guestService,settingsService,ISODateTimeFormat.basicDateTime().parseMillis(since)));
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to fetch updates"));
        }
    }
}
