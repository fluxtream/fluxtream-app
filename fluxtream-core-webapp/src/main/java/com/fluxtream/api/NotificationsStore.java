package com.fluxtream.api;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.SettingsModel;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.services.SettingsService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String discardNotifications(@RequestParam("ids") String ids)
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
