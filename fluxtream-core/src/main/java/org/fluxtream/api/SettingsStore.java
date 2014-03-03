package org.fluxtream.api;

import java.io.IOException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.domain.Guest;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.mvc.models.StatusModel;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.SettingsService;
import com.google.gson.Gson;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */

@Path("/settings")
@Component("RESTSettingsStore")
@Scope("request")
public class SettingsStore {

    @Autowired
    GuestService guestService;

    @Autowired
    SettingsService settingsService;

    private final Gson gson = new Gson();

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public String getSettings(){
        final long guestId = AuthHelper.getGuestId();
        final Guest guest = guestService.getGuestById(guestId);
        final GuestSettings settings = settingsService.getSettings(guestId);
        JSONObject jsonObject = JSONObject.fromObject(gson.toJson(settings));
        jsonObject.put("firstName", guest.firstname);
        jsonObject.put("lastName", guest.lastname);
        jsonObject.put("registrationMethod", guest.registrationMethod.name());
        jsonObject.put("username", guest.username);
        return jsonObject.toString();
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
    public String saveSettings(@FormParam("guest_firstname") String firstName, @FormParam("guest_lastname") String lastName) {
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
    public String saveSettings(@FormParam("length_measure_unit") String lengthUnit, @FormParam("distance_measure_unit") String distanceUnit,
                               @FormParam("weight_measure_unit") String weightUnit, @FormParam("temperature_unit") String temperatureUnit) {
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
    @Produces({ MediaType.APPLICATION_JSON })
    public String saveSettings(@FormParam("currentPassword") String currentPassword,
                               @FormParam("password1") String password1, @FormParam("password2") String password2)
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
