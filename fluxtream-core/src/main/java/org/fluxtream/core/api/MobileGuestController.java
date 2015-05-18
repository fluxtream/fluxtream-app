package org.fluxtream.core.api;

import com.wordnik.swagger.annotations.*;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.api.models.LoggedInGuestModel;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.auth.FlxUserDetails;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.GuestDetails;
import org.fluxtream.core.domain.oauth2.AuthorizationToken;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.OAuth2MgmtService;
import org.fluxtream.core.services.impl.ExistingEmailException;
import org.fluxtream.core.services.impl.UsernameAlreadyTakenException;
import org.joda.time.DateTime;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

/**
 * Created by candide on 22/12/14.
 */
@Component("RESTMobileGuestController")
@Scope("request")
@Path("/v1/mobile")
@Api(value = "/v1/mobile", description = "Mobile Signin & Registration API")
public class MobileGuestController {


    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    OAuth2MgmtService oAuth2MgmtService;

    @Autowired
    BeanFactory beanFactory;

    @GET
    @Path("/guest")
    @ApiOperation(value = "Get detailed info about ", response = LoggedInGuestModel.class)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLoggedInGuestInfo(@ApiParam(value="The device ID", required=true) @QueryParam("device_id") final String deviceId) {
        final Guest guest = AuthHelper.getGuest();
        final AuthorizationToken authorizationToken = oAuth2MgmtService.getAuthorizationToken(guest.getId(), deviceId, new DateTime().plusYears(1000).getMillis());
        final GuestDetails details = guestService.getGuestDetails(guest.getId());
        LoggedInGuestModel info = new LoggedInGuestModel(guest, details, authorizationToken.accessToken);
        return Response.ok(info).build();
    }

    @POST
    @Path("/signup")
    @ApiOperation(value = "Sign a new user up")
    @ApiResponses({
        @ApiResponse(code = 400, message = "If either username is already taken, email is already used or password is too short - " +
                "the reason is given in the response body")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(@ApiParam(value="E-mail", required=true) @FormParam("email") final String email,
                             @ApiParam(value="Username", required=true) @FormParam("username") final String username,
                             @ApiParam(value="First name", required=true) @FormParam("firstname") final String firstname,
                             @ApiParam(value="Last name", required=true) @FormParam("lastname") final String lastname,
                             @ApiParam(value="Password", required=true) @FormParam("password") final String password,
                             @ApiParam(value="The device ID", required=true) @FormParam("device_id") final String deviceId) throws URISyntaxException, UnsupportedEncodingException {
        if (password.length()<8) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Password is too short").build();
        }
        try {
            final Guest guest = guestService.createGuest(username, firstname, lastname, password, email, Guest.RegistrationMethod.REGISTRATION_METHOD_FORM, null);
            final AuthorizationToken authorizationToken = oAuth2MgmtService.getAuthorizationToken(guest.getId(), deviceId, new DateTime().plusYears(1000).getMillis());
            final GuestDetails details = guestService.getGuestDetails(guest.getId());
            LoggedInGuestModel info = new LoggedInGuestModel(guest, details, authorizationToken.accessToken);
            return Response.ok(info).build();
        } catch (ExistingEmailException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("This e-mail address is already used").build();
        } catch (UsernameAlreadyTakenException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("This username is already taken").build();
        }
    }

    @POST
    @Path("/signin")
    @ApiOperation(value = "Basic sign-in")
    @ApiResponses({
            @ApiResponse(code = 401, message = "on bad credentials")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response mobileSignin(@ApiParam(value="Username", required=true) @FormParam("username") final String usernameOrEmail,
                                 @ApiParam(value="Password", required=true) @FormParam("password") final String password,
                                 @ApiParam(value="The device ID", required=true) @FormParam("device_id") final String deviceId) throws UnsupportedEncodingException {
        Authentication authentication = getAuthentication(usernameOrEmail, password);
        if (authentication==null||!authentication.isAuthenticated()) {
            Guest guest = guestService.getGuestByEmail(usernameOrEmail);
            if (guest==null)
                return Response.status(Response.Status.UNAUTHORIZED).build();
            authentication = getAuthentication(guest.username, password);
        }
        if (authentication!=null&&authentication.isAuthenticated()) {
            FlxUserDetails principal = (FlxUserDetails) authentication.getPrincipal();
            Guest guest = principal.getGuest();
            final AuthorizationToken authorizationToken = oAuth2MgmtService.getAuthorizationToken(guest.getId(), deviceId, new DateTime().plusYears(1000).getMillis());
            final GuestDetails details = guestService.getGuestDetails(guest.getId());
            LoggedInGuestModel info = new LoggedInGuestModel(guest, details, authorizationToken.accessToken);
            return Response.ok(info).build();
        } else
            return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    private Authentication getAuthentication(String username, String password) {
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = null;
        try {authentication = authenticationManager.authenticate(authRequest);}
        catch (Throwable t) {/*ignore authentication errors*/}
        return authentication;
    }

}
