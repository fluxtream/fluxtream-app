package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.mvc.models.NotificationListModel;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.NotificationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */

@Path("/v1/notifications")
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
    @ApiOperation(value = "Post a notification (admins only)", response = String.class)
    @Secured("ROLE_ADMIN")
    public Response addNotification(@ApiParam(value="Guest's username", required=true) @PathParam("username") String username,
                                  @ApiParam(value="Message", required=true) @FormParam("message") String message,
                                  @ApiParam(value="Notification type",
                                          allowableValues = "INFO, WARNING, ERROR",
                                          required=true) @FormParam("type") String type)
            throws IOException {

        final Guest guest = guestService.getGuest(username);

        Notification.Type notificationType = Notification.Type.valueOf(type);

        notificationsService.addNotification(guest.getId(), notificationType, message);

        return Response.ok("notification posted").build();
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Delete a notification", response = String.class)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response discardNotification(@ApiParam(value="Notification ID", required=true) @PathParam("id") String idString)
            throws IOException {

        long guestId = AuthHelper.getGuestId();

        long id = Long.valueOf(idString);
        notificationsService.deleteNotification(guestId, id);

        return Response.ok("notification deleted").build();
    }

    @DELETE
    @ApiOperation(value = "Delete a list of notifications", response = String.class)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response discardNotifications(@ApiParam(value="Comma-separated list of notification ids", required=true) @QueryParam("ids") String ids)
            throws IOException {

        long guestId = AuthHelper.getGuestId();

        String[] idStrings = ids.split(",");
        for (String idString : idStrings) {
            long id = Long.valueOf(idString);
            notificationsService.deleteNotification(guestId, id);
        }

        return Response.ok("notifications deleted").build();
    }

    @GET
    @Path("/all")
    @ApiOperation(value = "Retrieve all current guest's notifications", response = NotificationListModel.class)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getAllNotifications()
            throws IOException {

        long guestId = AuthHelper.getGuestId();

        List<Notification> notifications = notificationsService.getNotifications(guestId);


        return Response.ok(gson.toJson(new NotificationListModel(notifications))).build();
    }


}
