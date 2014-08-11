package org.fluxtream.core.api;

import com.sun.jersey.api.Responses;
import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.oauth2.Application;
import org.fluxtream.core.domain.oauth2.AuthorizationToken;
import org.fluxtream.core.mvc.models.TechnicalAuthorizationTokenModel;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.OAuth2MgmtService;
import org.fluxtream.core.services.PartnerAppsService;
import org.fluxtream.core.services.impl.ExistingEmailException;
import org.fluxtream.core.services.impl.UsernameAlreadyTakenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * User: candide
 * Date: 11/07/14
 * Time: 11:37
 */
@Path("/v1/partners")
@Component("RESTPartnerRegisterController")
@Scope("request")
public class PartnerAppsRegisterController {

    @Autowired
    OAuth2MgmtService oAuth2MgmtService;

    @Autowired
    PartnerAppsService partnerAppsService;

    @Autowired
    GuestService guestService;

    @POST
    @Path("/apps/{appSecret}/guests")
    public Response register(@PathParam("appSecret") final String appSecret,
                             @FormParam("email") final String email,
                             @FormParam("username") final String username,
                             @FormParam("firstname") final String firstname,
                             @FormParam("lastname") final String lastname) throws IOException {
        final Application application = partnerAppsService.getApplication(appSecret);
        if (application==null)
            return Responses.notFound().build();
        if (!application.registrationAllowed)
            return Response.status(Response.Status.FORBIDDEN).build();
        try {
            final Guest guest = guestService.createGuest(username, firstname, lastname, null, email, Guest.RegistrationMethod.REGISTRATION_METHOD_API, application.uid);
            final AuthorizationToken authorizationToken = oAuth2MgmtService.issueAuthorizationToken(guest.getId(), application.getId());
            TechnicalAuthorizationTokenModel authorizationTokenModel = new TechnicalAuthorizationTokenModel(authorizationToken, guest);
            final String json = (new ObjectMapper()).writeValueAsString(authorizationTokenModel);
            return Response.ok(json).build();
        } catch (UsernameAlreadyTakenException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("This username is already taken").build();
        } catch (ExistingEmailException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("This e-mail address is already used").build();
        }
    }

}
