package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.*;
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
    @ApiOperation(value = "Post a notification (admins only)")
    @ApiResponses({
            @ApiResponse(code=200, message = "Notification posted")
    })
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

        return Response.ok("Notification posted").build();
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Delete a notification")
    @ApiResponses({
            @ApiResponse(code=200, message = "Notification deleted")
    })
    public Response discardNotification(@ApiParam(value="Notification ID", required=true) @PathParam("id") String idString)
            throws IOException {

        long guestId = AuthHelper.getGuestId();

        long id = Long.valueOf(idString);
        notificationsService.deleteNotification(guestId, id);

        return Response.ok("Notification deleted").build();
    }

    @DELETE
    @ApiOperation(value = "Delete a list of notifications")
    @ApiResponses({
            @ApiResponse(code=200, message = "Notifications deleted")
    })
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
