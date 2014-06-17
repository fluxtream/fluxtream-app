package org.fluxtream.core.api;

import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/v1/test")
@Component("RESTTestController")
@Scope("request")
public class TestController {

    @Autowired
    GuestService guestService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @GET
    @Path("/{username}/setAttribute")
    @Produces({MediaType.APPLICATION_JSON})
    public String setAttribute(@Context HttpServletRequest request,
                               @Context HttpServletResponse response,
                               @PathParam("username") String username,
                               @QueryParam("att") String attValue) throws IOException {
        // check that we're running locally
        if (!RequestUtils.isDev(request)) {
            response.setStatus(403);
        }
        final Guest guest = guestService.getGuest(username);
        ApiKey apiKey = guestService.getApiKey(guest.getId(), Connector.getConnector("fluxtream_capture"));
        if (apiKey == null) {
            apiKey = guestService.createApiKey(guest.getId(), Connector.getConnector("fluxtream_capture"));
        }
        guestService.setApiKeyAttribute(apiKey, "test", attValue);
        return "attribute was set";
    }

    @GET
    @Path("/update/{username}/{connectorName}")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateUserConnector(@Context HttpServletRequest request,
                                      @Context HttpServletResponse response,
                                      @PathParam("username") String username,
                                      @PathParam("connectorName") String connectorName) throws IOException {
        // check that we're running locally
        if (!RequestUtils.isDev(request)) {
            response.setStatus(403);
        }
        final Guest guest = guestService.getGuest(username);
        final Connector connector = Connector.getConnector(connectorName);
        ApiKey apiKey = guestService.getApiKey(guest.getId(), connector);
        connectorUpdateService.updateConnector(apiKey, true);
        return "updating connector " + connectorName + " for guest " + guest.username;
    }

    @GET
    @Path("/update/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateUserConnectors(@Context HttpServletRequest request,
                                       @Context HttpServletResponse response,
                                       @PathParam("username") String username) throws IOException {
        // check that we're running locally
        if (!RequestUtils.isDev(request)) {
            response.setStatus(403);
        }
        final Guest guest = guestService.getGuest(username);
        connectorUpdateService.updateAllConnectors(guest.getId(), true);
        return "updating all connectors for guest " + guest.username;
    }

    @GET
    @Path("/ping")
    @Produces({MediaType.TEXT_PLAIN})
    public String ping() throws IOException {
        return "pong";
    }

    @GET
    @Path("/statusCode/{statusCode}")
    @Produces({MediaType.TEXT_PLAIN})
    public Response testStatusCode(@PathParam("statusCode") int statusCode) throws IOException {
        return Response.status(statusCode).entity("Some human-readable message").build();
    }

}
