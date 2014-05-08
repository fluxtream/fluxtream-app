package org.fluxtream.core.api;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.mvc.models.NotificationListModel;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.mvc.models.guest.GuestModel;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.NotificationsService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */

@Path("/notifications")
@Component("RESTNotificationsStore")
@Api(value = "/notifications", description = "Post a notification to a guest (admins only), retrieve and discard notifications")
@Scope("request")
public class NotificationsStore {

    @Autowired
    GuestService guestService;

    @Autowired
    NotificationsService notificationsService;

    private final Gson gson = new Gson();


    @POST
    @Path("/{username}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Post a notification (admins only)", response = StatusModel.class)
    @Secured("ROLE_ADMIN")
    public String addNotification(@ApiParam(value="Guest's username", required=true) @PathParam("username") String username,
                                  @ApiParam(value="Message", required=true) @FormParam("message") String message,
                                  @ApiParam(value="Notification type",
                                          allowableValues = "INFO, WARNING, ERROR",
                                          required=true) @FormParam("type") String type)
            throws IOException {

        final Guest guest = guestService.getGuest(username);

        Notification.Type notificationType = Notification.Type.valueOf(type);

        notificationsService.addNotification(guest.getId(), notificationType, message);

        StatusModel status = new StatusModel(true, "notification posted");
        String json = gson.toJson(status);

        return json;
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Delete a notification", response = StatusModel.class)
    @Produces({ MediaType.APPLICATION_JSON })
    public String discardNotification(@ApiParam(value="Notification ID", required=true) @PathParam("id") String idString)
            throws IOException {

        long guestId = AuthHelper.getGuestId();

        long id = Long.valueOf(idString);
        notificationsService.deleteNotification(guestId, id);

        StatusModel status = new StatusModel(true, "notification deleted");
        String json = gson.toJson(status);

        return json;
    }

    @DELETE
    @ApiOperation(value = "Delete a list of notifications", response = StatusModel.class, responseContainer = "array")
    @Produces({ MediaType.APPLICATION_JSON })
    public String discardNotifications(@ApiParam(value="Comma-separated list of notification ids", required=true) @QueryParam("ids") String ids)
            throws IOException {

        long guestId = AuthHelper.getGuestId();

        String[] idStrings = ids.split(",");
        for (String idString : idStrings) {
            long id = Long.valueOf(idString);
            notificationsService.deleteNotification(guestId, id);
        }

        StatusModel status = new StatusModel(true, "notifications deleted");
        String json = gson.toJson(status);

        return json;
    }

    @GET
    @Path("/all")
    @ApiOperation(value = "Retrieve all current guest's notifications", response = NotificationListModel.class)
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAllNotifications()
            throws IOException {

        long guestId = AuthHelper.getGuestId();

        List<Notification> notifications = notificationsService.getNotifications(guestId);


        return gson.toJson(new NotificationListModel(notifications));
    }


}
