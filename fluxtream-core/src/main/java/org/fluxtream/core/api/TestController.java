package org.fluxtream.core.api;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import org.apache.commons.io.IOUtils;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.updaters.AuthExpiredException;
import org.fluxtream.core.connectors.updaters.RateLimitReachedException;
import org.fluxtream.core.connectors.updaters.UnexpectedResponseCodeException;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
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
import java.net.HttpURLConnection;
import java.net.URL;

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

    @Autowired
    Configuration env;

    public static final String SUBSCRIBE_TO_FITBIT_NOTIFICATIONS_CALL = "SUBSCRIBE_TO_FITBIT_NOTIFICATIONS_CALL";

    static {
        ObjectType.registerCustomObjectType(SUBSCRIBE_TO_FITBIT_NOTIFICATIONS_CALL);
    }


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
    @Path("/fitbit/addSubscription")
    @Produces({MediaType.APPLICATION_JSON})
    public Response addFitbitSubscription(@Context HttpServletRequest request,
                                          @Context HttpServletResponse response) throws IOException, UpdateFailedException, UnexpectedResponseCodeException, RateLimitReachedException, AuthExpiredException {
        // check that we're running locally
        if (!RequestUtils.isDev(request)) {
            response.setStatus(403);
        }
        String fitbitSubscriberId = env.get("fitbitSubscriberId");
        final Guest guest = AuthHelper.getGuest();
        ApiKey apiKey = guestService.getApiKey(guest.getId(), Connector.getConnector("fitbit"));
        final String fitbitResponse = makeRestCall(apiKey, ObjectType.getCustomObjectType(SUBSCRIBE_TO_FITBIT_NOTIFICATIONS_CALL).value(),
                "https://api.fitbit.com/1/user/-/apiSubscriptions/" + fitbitSubscriberId + ".json", "POST");
//        final String fitbitResponse = makeRestCall(apiKey, ObjectType.getCustomObjectType(SUBSCRIBE_TO_FITBIT_NOTIFICATIONS_CALL).value(),
//                "https://api.fitbit.com/1/user/-/activities/date/2013-02-25.json");
        return Response.ok().entity(fitbitResponse).build();
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

    public final String makeRestCall(final ApiKey apiKey,
                                     final int objectTypes, final String urlString, final String...method)
            throws RateLimitReachedException, UpdateFailedException, AuthExpiredException, UnexpectedResponseCodeException {


        try {
            long then = System.currentTimeMillis();
            URL url = new URL(urlString);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            if (method!=null && method.length>0)
                request.setRequestMethod(method[0]);

            OAuthConsumer consumer = new DefaultOAuthConsumer(
                    getConsumerKey(apiKey), getConsumerSecret(apiKey));

            consumer.setTokenWithSecret(
                    guestService.getApiKeyAttribute(apiKey,"accessToken"),
                    guestService.getApiKeyAttribute(apiKey,"tokenSecret"));

            // sign the request (consumer is a Signpost DefaultOAuthConsumer)
            try {
                consumer.sign(request);
            } catch (Exception e) {
                throw new RuntimeException("OAuth exception: " + e.getMessage());
            }
            request.connect();
            final int httpResponseCode = request.getResponseCode();

            final String httpResponseMessage = request.getResponseMessage();

            if (httpResponseCode == 200) {
                String json = IOUtils.toString(request.getInputStream());
                connectorUpdateService.addApiUpdate(apiKey,
                        objectTypes, then, System.currentTimeMillis() - then,
                        urlString, true, httpResponseCode, httpResponseMessage);
                // logger.info(updateInfo.apiKey.getGuestId(), "REST call success: " +
                // urlString);
                return json;
            } else {
                connectorUpdateService.addApiUpdate(apiKey,
                        objectTypes, then, System.currentTimeMillis() - then,
                        urlString, false, httpResponseCode, httpResponseMessage);
                // Check for response code 429 which is Fitbit's over rate limit error
                if(httpResponseCode == 429) {
                    // try to retrieve the reset time from Fitbit, otherwise default to a one hour delay
                    throw new RateLimitReachedException();
                }
                else {
                    // Otherwise throw the same error that SignpostOAuthHelper used to throw
                    if (httpResponseCode == 401)
                        throw new AuthExpiredException();
                    else if (httpResponseCode==429)
                        throw new UnexpectedResponseCodeException(429, httpResponseMessage, urlString);
                    else if (httpResponseCode >= 400 && httpResponseCode < 500)
                        throw new UpdateFailedException("Unexpected response code: " + httpResponseCode, true,
                                ApiKey.PermanentFailReason.clientError(httpResponseCode));
                    throw new UpdateFailedException(false, "Error: " + httpResponseCode);
                }
            }
        } catch (IOException exc) {
            throw new RuntimeException("IOException trying to make rest call: " + exc.getMessage());
        }
    }

    private String getConsumerSecret(ApiKey apiKey) {
        String consumerSecret = guestService.getApiKeyAttribute(apiKey, apiKey.getConnector().getName() + "ConsumerSecret");
        return consumerSecret == null ? "" : consumerSecret;
    }

    private String getConsumerKey(ApiKey apiKey) {
        String consumerKey = guestService.getApiKeyAttribute(apiKey, apiKey.getConnector().getName() + "ConsumerKey");
        return consumerKey == null ? "" : consumerKey;
    }

}
