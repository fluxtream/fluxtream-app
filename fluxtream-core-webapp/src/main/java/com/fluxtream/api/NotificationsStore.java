package com.fluxtream.api;

import java.io.IOException;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.NotificationsService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */

@Path("/notifications")
@Component("RESTNotificationsStore")
@Scope("request")
public class NotificationsStore {

    @Autowired
    NotificationsService notificationsService;

    private final Gson gson = new Gson();

    @DELETE
    @Produces({ MediaType.APPLICATION_JSON })
    public String discardNotifications(@QueryParam("ids") String ids)
            throws IOException {

        long guestId = ControllerHelper.getGuestId();

        String[] idStrings = ids.split(",");
        for (String idString : idStrings) {
            long id = Long.valueOf(idString);
            notificationsService.deleteNotification(guestId, id);
        }

        StatusModel status = new StatusModel(true, "notifications deleted");
        String json = gson.toJson(status);

        return json;
    }


}
