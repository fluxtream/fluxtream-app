package com.fluxtream.api;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.Configuration;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.GuestService;
import com.fluxtream.domain.GuestAddress;

import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.utils.HttpUtils;
import com.google.gson.Gson;
import com.fluxtream.services.SettingsService;
import com.fluxtream.services.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.util.List;


@Path("/guest/{username}/address")
@Component("addressApi")
@Scope("request")
public class AddressResource {

    Gson gson = new Gson();

    @Autowired
    SettingsService settingsService;

    @Autowired
    GuestService guestService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    Configuration env;


    @GET
    @Path("/all")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAddresses(@PathParam("username") String username){
        try{
            Guest guest = guestService.getGuest(username);
            List<GuestAddress> addresses = settingsService.getAllAddresses(guest.getId());
            return gson.toJson(addresses);
        } catch (Exception e) {
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    @GET
    @Path("/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAddress(@PathParam("username") String username, @PathParam("index") int index){
        try{
            Guest guest = guestService.getGuest(username);
            List<GuestAddress> addresses = settingsService.getAllAddresses(guest.getId());
            return gson.toJson(addresses.get(index));
        } catch (Exception e) {
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    private List<GuestAddress> getAddressesOfType(Guest guest, String type){
        return settingsService.getAllAddressesOfType(guest.getId(),type);
    }

    private List<GuestAddress> getAddressesAtDate(Guest guest, String date){
        DayMetadataFacet dayMeta = metadataService.getDayMetadata(guest.getId(),date,true);
        return settingsService.getAllAddressesForDate(guest.getId(),(dayMeta.start + dayMeta.end)/2);
    }

    @GET
    @Path("/{selector}/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAddressBySingleSelector(@PathParam("username") String username, @PathParam("selector") String selector, @PathParam("index") int index){
        try{
            Guest guest = guestService.getGuest(username);
            List<GuestAddress> addresses;
            try{
                addresses = getAddressesAtDate(guest,selector);
            } catch (Exception e) {
                addresses = getAddressesOfType(guest,selector);
            }
            return gson.toJson(addresses.get(index));
        } catch (Exception e) {
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    @GET
    @Path("/{selector}/all")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAddressBySingleSelector(@PathParam("username") String username, @PathParam("selector") String selector){
        try{
            Guest guest = guestService.getGuest(username);
            List<GuestAddress> addresses;
            try{
                addresses = getAddressesAtDate(guest,selector);
            } catch (Exception e) {
                addresses = getAddressesOfType(guest,selector);
            }
            return gson.toJson(addresses);
        } catch (Exception e) {
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }


    @GET
    @Path("/{type}/{date}/all")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAddressesOfTypeAtDate(@PathParam("username") String username, @PathParam("type") String type, @PathParam("date") String date){
        try{
            Guest guest = guestService.getGuest(username);
            DayMetadataFacet dayMeta = metadataService.getDayMetadata(guest.getId(),date,true);
            List<GuestAddress> addresses = settingsService.getAllAddressesOfTypeForDate(guest.getId(),type,(dayMeta.start + dayMeta.end)/2);
            return gson.toJson(addresses);
        } catch (Exception e) {
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    @GET
    @Path("/{type}/{date}/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAddressesOfTypeAtDate(@PathParam("username") String username, @PathParam("type") String type, @PathParam("date") String date, @PathParam("index") int index){
        try{
            Guest guest = guestService.getGuest(username);
            DayMetadataFacet dayMeta = metadataService.getDayMetadata(guest.getId(),date,true);
            List<GuestAddress> addresses = settingsService.getAllAddressesOfTypeForDate(guest.getId(),type,(dayMeta.start + dayMeta.end)/2);
            return gson.toJson(addresses.get(index));
        } catch (Exception e) {
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    @POST
    @Path("/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String addAddress(@PathParam("type") String type, @FormParam("address") String address, @FormParam("latitude") double latitude,
                             @FormParam("longitude") double longitude, @FormParam("since") String since, @FormParam("until") String until,
                             @PathParam("username") String username){
        try{
            Guest guest = guestService.getGuest(username);
            long startTime, endTime = 0;
            try{
                DayMetadataFacet dayMeta = metadataService.getDayMetadata(guest.getId(),since,true);
                startTime = dayMeta.start;
            } catch (Exception e){
                startTime = Long.parseLong(since);
            }
            if (until != null){
                try{
                    DayMetadataFacet dayMeta = metadataService.getDayMetadata(guest.getId(),until,true);
                    endTime = dayMeta.end;
                } catch (Exception e){
                    endTime = Long.parseLong(until);
                }
            }

            String addressEncoded = URLEncoder.encode(address, "UTF-8");
            String jsonString = HttpUtils.fetch("https://maps.googleapis.com/maps/api/geocode/json?sensor=false&address=" + addressEncoded, env);

            if (until != null)
                settingsService.addAddress(guest.getId(),type,address,latitude,longitude,startTime,endTime,jsonString);
            else
                settingsService.addAddress(guest.getId(),type,address,latitude,longitude,startTime,jsonString);

            return gson.toJson(new StatusModel(true, "Successfully added guest address"));
        } catch (Exception e) {
            StatusModel result = new StatusModel(false, "Could not add guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }
}
