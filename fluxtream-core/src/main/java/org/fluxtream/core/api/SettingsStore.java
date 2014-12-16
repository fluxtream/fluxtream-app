package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.*;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.AuthorizationTokenModel;
import org.fluxtream.core.mvc.models.GuestSettingsModel;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.OAuth2MgmtService;
import org.fluxtream.core.services.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
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

@Path("/v1/settings")
@Component("RESTSettingsStore")
@Api(value = "/settings", description = "Set and retrieve the user's preferences")
@Scope("request")
public class SettingsStore {

    @Autowired
    GuestService guestService;

    @Autowired
    SettingsService settingsService;

    @Autowired
    OAuth2MgmtService oAuth2MgmtService;

    private final Gson gson = new Gson();

    @DELETE
    @Path("/accessTokens/{accessToken}")
    @ApiOperation(value = "Get the user's settings", response = GuestSettingsModel.class)
    public Response revokeAccessToken(@PathParam("accessToken") final String accessToken){
        final long guestId = AuthHelper.getGuestId();
        try {
            oAuth2MgmtService.revokeAccessToken(guestId, accessToken);
            return Response.ok("Successfully deleted authToken with accessToken " + accessToken).build();
        } catch (Throwable t) {
            return Response.serverError().entity(("Couldn't remove accessToken")).build();
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get the user's settings", response = GuestSettingsModel.class)
    public GuestSettingsModel getSettings(){
        final long guestId = AuthHelper.getGuestId();
        final Guest guest = guestService.getGuestById(guestId);
        final GuestSettings settings = settingsService.getSettings(guestId);
        final List<AuthorizationTokenModel> accessTokens = oAuth2MgmtService.getTokens(guestId);
        GuestSettingsModel settingsModel = new GuestSettingsModel(settings, guest.username,
                guest.firstname, guest.lastname, guest.registrationMethod, accessTokens);
        return settingsModel;
    }

    @POST
    @Path("/{messageName}/increment")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response incrementCounter(@PathParam("messageName") String messageName) {
        final long guestId = AuthHelper.getGuestId();
        try {
            final int count = settingsService.incrementDisplayCounter(guestId, messageName);
            StatusModel status = new StatusModel(true, String.format("incremented message '%s' to %s", messageName, count));
            status.payload = count;
            return Response.ok(status).build();
        }
        catch (Throwable e) {
            e.printStackTrace();
            return Response.serverError().entity(String.format("Could not increment counter for message '%s'", messageName)).build();
        }
    }

    @POST
    @Path("/deleteAccount")
    @ApiOperation(value = "Delete the user's account (this is irreversible)")
    @ApiResponses({
            @ApiResponse(code=200, message = "Successfully deleted account")
    })
    public Response eraseEverything() {
        final long guestId = AuthHelper.getGuestId();
        try {
            guestService.eraseGuestInfo(guestId);
        }
        catch (Throwable e) {
            e.printStackTrace();
            return Response.serverError().entity("There was an unexpected error " +
                    "while deleting your account: " + e.getMessage() +
                    " - Please contact your administrator").build();
        }
        return Response.ok("Successfully deleted account").build();
    }

    @POST
    @Path("/general")
    @ApiOperation(value = "Set the user's first name and last name")
    @ApiResponses({
            @ApiResponse(code=200, message = "Settings updated")
    })
    public Response saveSettings(@ApiParam(value="First name", required=true) @FormParam("guest_firstname") String firstName,
                               @ApiParam(value="Last name", required=true) @FormParam("guest_lastname") String lastName) {
        try {
            Guest guest = AuthHelper.getGuest();

            settingsService.setFirstname(guest.getId(), firstName);
            settingsService.setLastname(guest.getId(), lastName);

            return Response.ok("settings updated!").build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to save settings: " + e.getMessage()).build();
        }
    }


    @POST
    @Path("/units")
    @ApiOperation(value = "Set the user's preferred units of measure")
    @ApiResponses({
            @ApiResponse(code=200, message = "Settings updated!")
    })
    public Response saveSettings(@ApiParam(value="Length measure unit", allowableValues = "SI, FEET_INCHES", required=true) @FormParam("length_measure_unit") String lengthUnit,
                               @ApiParam(value="Distance measure unit", allowableValues = "SI, MILES_YARDS", required=true) @FormParam("distance_measure_unit") String distanceUnit,
                               @ApiParam(value="Weight measure unit", allowableValues = "SI, POUNDS, STONES", required=true) @FormParam("weight_measure_unit") String weightUnit,
                               @ApiParam(value="Temperature unit", allowableValues = "CELSIUS, FAHRENHEIT", required=true) @FormParam("temperature_unit") String temperatureUnit) {
        try{

            Guest guest = AuthHelper.getGuest();
            GuestSettings.LengthMeasureUnit lngUnt = Enum.valueOf(
                    GuestSettings.LengthMeasureUnit.class, lengthUnit);
            GuestSettings.DistanceMeasureUnit dstUnt = Enum.valueOf(
                    GuestSettings.DistanceMeasureUnit.class, distanceUnit);
            GuestSettings.WeightMeasureUnit whtUnt = Enum.valueOf(
                    GuestSettings.WeightMeasureUnit.class, weightUnit);
            GuestSettings.TemperatureUnit tempUnt = Enum.valueOf(
                    GuestSettings.TemperatureUnit.class, temperatureUnit);
            settingsService.setLengthMeasureUnit(guest.getId(), lngUnt);
            settingsService.setDistanceMeasureUnit(guest.getId(), dstUnt);
            settingsService.setWeightMeasureUnit(guest.getId(), whtUnt);
            settingsService.setTemperatureUnit(guest.getId(), tempUnt);

            return Response.ok("settings updated!").build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to save settings: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/password")
    @ApiOperation(value = "Reset password", response = StatusModel.class)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response saveSettings(@ApiParam(value="Current password", required=true) @FormParam("currentPassword") String currentPassword,
                               @ApiParam(value="New password", required=true) @FormParam("password1") String password1,
                               @ApiParam(value="New password (repeat)", required=true) @FormParam("password2") String password2)
            throws IOException {
        try{
            Guest guest = AuthHelper.getGuest();

            if (currentPassword!=null) {
                boolean passwordMatched = guestService.checkPassword(guest.getId(), currentPassword);
                if (!passwordMatched) {
                    return Response.ok(gson.toJson(new StatusModel(false, "Wrong Password"))).build();
                }
            }

            if (password1.length()==0 || password2.length()==0)
                return Response.ok(gson.toJson(new StatusModel(false, "Please fill in both password fields"))).build();
            else {
                if (!password1.equals(password2)) {
                    return Response.ok(gson.toJson(new StatusModel(false, "Passwords don't match"))).build();
                }
                if (password1.length()<8) {
                    return Response.ok(gson.toJson(new StatusModel(false, "Your password should be at least 8 characters long"))).build();
                } else {
                    guestService.setPassword(guest.getId(), password1);
                }
            }

            return Response.ok(new StatusModel(true, "settings updated!")).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to save settings: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/preferences")
    @ApiOperation(value="Save miscellaneous preferences")
    @Consumes("text/plain")
    public Response savePreferences(@ApiParam(value="Raw preferences JSON", required=true) String prefsJSON) {
        settingsService.setPreferences(AuthHelper.getGuestId(), prefsJSON);
        return Response.ok().build();
    }

}
