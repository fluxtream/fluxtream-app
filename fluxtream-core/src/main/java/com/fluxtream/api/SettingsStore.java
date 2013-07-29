package com.fluxtream.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.SettingsModel;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;
import com.google.gson.Gson;
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
    public String getSettings() {
        try{
            Guest guest = guestService.getGuestById(AuthHelper.getGuestId());
            GuestSettings settings = settingsService.getSettings(guest.getId());
            final List<ApiKey> guestsApiKeys = guestService.getApiKeys(guest.getId());
            Map<Long,Object> apiKeySettings = new HashMap<Long,Object>();
            for (ApiKey guestApiKey : guestsApiKeys) {
                final Object connectorSettings = settingsService.getConnectorSettings(guestApiKey.getId());
                if (connectorSettings!=null)
                    apiKeySettings.put(guestApiKey.getId(), connectorSettings);
            }
            return gson.toJson(new SettingsModel(settings,apiKeySettings, guest));
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get settings: " + e.getMessage()));
        }
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    public String saveSettings(@FormParam("guest_firstname") String firstName, @FormParam("guest_lastname") String lastName,
                               @FormParam("length_measure_unit") String lengthUnit, @FormParam("distance_measure_unit") String distanceUnit,
                               @FormParam("weight_measure_unit") String weightUnit, @FormParam("temperature_unit") String temperatureUnit,
                               @FormParam("password1") String password1, @FormParam("password2") String password2) throws IOException {
        try{
            GuestSettings.LengthMeasureUnit lngUnt = Enum.valueOf(
                    GuestSettings.LengthMeasureUnit.class, lengthUnit);
            GuestSettings.DistanceMeasureUnit dstUnt = Enum.valueOf(
                    GuestSettings.DistanceMeasureUnit.class, distanceUnit);
            GuestSettings.WeightMeasureUnit whtUnt = Enum.valueOf(
                    GuestSettings.WeightMeasureUnit.class, weightUnit);
            GuestSettings.TemperatureUnit tempUnt = Enum.valueOf(
                    GuestSettings.TemperatureUnit.class, temperatureUnit);

            long guestId = AuthHelper.getGuestId();

            settingsService.setLengthMeasureUnit(guestId, lngUnt);
            settingsService.setDistanceMeasureUnit(guestId, dstUnt);
            settingsService.setWeightMeasureUnit(guestId, whtUnt);
            settingsService.setTemperatureUnit(guestId, tempUnt);

            settingsService.setFirstname(guestId, firstName);
            settingsService.setLastname(guestId, lastName);

            if (password1.length()==0&&password2.length()>0)
                return gson.toJson(new StatusModel(false, "Please fill in both password fields"));
            else if (password2.length()==0&&password1.length()>0)
                return gson.toJson(new StatusModel(false, "Password verification is required"));
            else if (!(password1.length()==0&&password2.length()==0)) {
                if (!password1.equals(password2)) {
                    return gson.toJson(new StatusModel(false, "Passwords don't match"));
                }
                if (password1.length()<8) {
                    return gson.toJson(new StatusModel(false, "Your password should be at least 8 characters long"));
                } else {
                    guestService.setPassword(guestId, password1);
                }
            }

            StatusModel status = new StatusModel(true, "settings updated!");
            return gson.toJson(status);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to save settings: " + e.getMessage()));
        }
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/reset/{apiKeyId}")
    public String resetConncetorSettings(@PathParam("apiKeyId") long apiKeyId) {
        settingsService.resetConnectorSettings(apiKeyId);
        StatusModel status = new StatusModel(true, "connector settings reset!");
        return gson.toJson(status);
    }


}
