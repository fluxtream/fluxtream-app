package org.fluxtream.api;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.Guest;
import org.fluxtream.services.ConnectorUpdateService;
import org.fluxtream.services.GuestService;
import org.fluxtream.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/test")
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

}
