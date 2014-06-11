package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
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
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */

@Path("/settings")
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
    public StatusModel incrementCounter(@PathParam("messageName") String messageName) {
        final long guestId = AuthHelper.getGuestId();
        try {
            final int count = settingsService.incrementDisplayCounter(guestId, messageName);
            StatusModel status = new StatusModel(true, String.format("incremented message '%s' to %s", messageName, count));
            status.payload = count;
            return status;
        }
        catch (Throwable e) {
            e.printStackTrace();
            return new StatusModel(false, String.format("Could not increment counter for message '%s'", messageName));
        }
    }

    @POST
    @Path("/deleteAccount")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Delete the user's account (this is irreversible)", response = StatusModel.class)
    public StatusModel eraseEverything() {
        final long guestId = AuthHelper.getGuestId();
        try {
            guestService.eraseGuestInfo(guestId);
        }
        catch (Throwable e) {
            e.printStackTrace();
            return new StatusModel(false, "There was an unexpected error " +
                                          "while deleting your account: " + e.getMessage() +
                                          " - Please contact your administrator");
        }
        return new StatusModel(true, "Successfully deleted account");
    }

    @POST
    @Path("/general")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Set the user's first name and last name", response = StatusModel.class)
    public String saveSettings(@ApiParam(value="First name", required=true) @FormParam("guest_firstname") String firstName,
                               @ApiParam(value="Last name", required=true) @FormParam("guest_lastname") String lastName) {
        try {
            Guest guest = AuthHelper.getGuest();

            settingsService.setFirstname(guest.getId(), firstName);
            settingsService.setLastname(guest.getId(), lastName);

            StatusModel status = new StatusModel(true, "settings updated!");
            return gson.toJson(status);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to save settings: " + e.getMessage()));
        }
    }


    @POST
    @Path("/units")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Set the user's preferred units of measure", response = StatusModel.class)
    public String saveSettings(@ApiParam(value="Length measure unit", allowableValues = "SI, FEET_INCHES", required=true) @FormParam("length_measure_unit") String lengthUnit,
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

            StatusModel status = new StatusModel(true, "settings updated!");
            return gson.toJson(status);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to save settings: " + e.getMessage()));
        }
    }

    @POST
    @Path("/password")
    @ApiOperation(value = "Reset password", response = StatusModel.class)
    @Produces({ MediaType.APPLICATION_JSON })
    public String saveSettings(@ApiParam(value="Current password", required=true) @FormParam("currentPassword") String currentPassword,
                               @ApiParam(value="New password", required=true) @FormParam("password1") String password1,
                               @ApiParam(value="New password (repeat)", required=true) @FormParam("password2") String password2)
            throws IOException {
        try{
            Guest guest = AuthHelper.getGuest();

            if (currentPassword!=null) {
                boolean passwordMatched = guestService.checkPassword(guest.getId(), currentPassword);
                if (!passwordMatched) {
                    return gson.toJson(new StatusModel(false, "Wrong Password"));
                }
            }

            if (password1.length()==0 || password2.length()==0)
                return gson.toJson(new StatusModel(false, "Please fill in both password fields"));
            else {
                if (!password1.equals(password2)) {
                    return gson.toJson(new StatusModel(false, "Passwords don't match"));
                }
                if (password1.length()<8) {
                    return gson.toJson(new StatusModel(false, "Your password should be at least 8 characters long"));
                } else {
                    guestService.setPassword(guest.getId(), password1);
                }
            }

            StatusModel status = new StatusModel(true, "settings updated!");
            return gson.toJson(status);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to save settings: " + e.getMessage()));
        }
    }

}
