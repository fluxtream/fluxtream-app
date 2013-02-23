package com.fluxtream.api;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.RequestUtils;
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

    @GET
    @Path("/setAttribute")
    @Produces({MediaType.APPLICATION_JSON})
    public String setAttribute(@Context HttpServletRequest request,
                               @Context HttpServletResponse response,
                               @QueryParam("att") String attValue) throws IOException {
        // check that we're running locally
        if (!RequestUtils.isDev(request)) {
            response.setStatus(403);
        }
        final long guestId = AuthHelper.getGuestId();
        ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector("fluxtream_capture"));
        if (apiKey==null) {
            apiKey = guestService.createApiKey(guestId, Connector.getConnector("fluxtream_capture"));
        }
        guestService.setApiKeyAttribute(apiKey, "test", attValue);
        return "attribute was set";
    }

}
