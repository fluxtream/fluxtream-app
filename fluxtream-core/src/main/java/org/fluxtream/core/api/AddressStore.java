package org.fluxtream.core.api;

import java.net.URLEncoder;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.GuestAddress;
import org.fluxtream.core.metadata.DayMetadata;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.SettingsService;
import org.fluxtream.core.utils.HttpUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/addresses")
@Component("RESTAddressStore")
@Scope("request")
public class AddressStore {

    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Autowired
    SettingsService settingsService;

    @Autowired
    GuestService guestService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    Configuration env;


    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public String getAddresses(){
        try{
            Guest guest = AuthHelper.getGuest();
            List<GuestAddress> addresses = settingsService.getAllAddresses(guest.getId());
            return gson.toJson(addresses);
        } catch (Exception e) {
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    /**
     * get an address
     * @param index
     * @return
     */
    @GET
    @Path("/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAddress(@PathParam("index") int index){
        try{
            Guest guest = AuthHelper.getGuest();
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
        DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), date);
        return settingsService.getAllAddressesForDate(guest.getId(),(dayMeta.start + dayMeta.end)/2);
    }

    @GET
    @Path("/{selector}/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAddressBySingleSelector(@PathParam("selector") String selector, @PathParam("index") int index){
        try{
            Guest guest = AuthHelper.getGuest();
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
    public String getAddressBySingleSelector(@PathParam("selector") String selector){
        try{
            Guest guest = AuthHelper.getGuest();
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
    public String getAddressesOfTypeAtDate(@PathParam("type") String type, @PathParam("date") String date){
        try{
            Guest guest = AuthHelper.getGuest();
            DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), date);
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
    public String getAddressesOfTypeAtDate(@PathParam("type") String type, @PathParam("date") String date, @PathParam("index") int index){
        try{
            Guest guest = AuthHelper.getGuest();
            DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), date);
            List<GuestAddress> addresses = settingsService.getAllAddressesOfTypeForDate(guest.getId(),type,(dayMeta.start + dayMeta.end)/2);
            return gson.toJson(addresses.get(index));
        } catch (Exception e) {
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    public String addAddress(String type, String address, double latitude, double longitude, double radius, String since,
                             String until, String username){
        try{
            Guest guest = guestService.getGuest(username);
            long startTime, endTime = 0;
            try{
                DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), since);
                startTime = dayMeta.start;
            } catch (Exception e){
                startTime = Long.parseLong(since);
            }
            if (until != null){
                if (until.equalsIgnoreCase("Present"))
                    endTime = Long.MAX_VALUE;
                else{
                    try{
                        DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), until);
                        endTime = dayMeta.end;
                    } catch (Exception e){
                        endTime = Long.parseLong(until);
                    }
                }
            }

            String addressEncoded = URLEncoder.encode(address, "UTF-8");
            String jsonString = HttpUtils.fetch("https://maps.googleapis.com/maps/api/geocode/json?sensor=false&address=" + addressEncoded);

            GuestAddress newAddress;

            if (until != null)
                newAddress = settingsService.addAddress(guest.getId(),type,address,latitude,longitude,startTime,endTime,radius,jsonString);
            else
                newAddress = settingsService.addAddress(guest.getId(),type,address,latitude,longitude,startTime,radius,jsonString);

            return gson.toJson(new StatusModel(true, gson.toJson(newAddress)));
        } catch (Exception e) {
            StatusModel result = new StatusModel(false, "Could not add guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    @POST
    @Path("/{input}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String addUpdateAddress(@PathParam("input") String input, @FormParam("address") String address, @FormParam("latitude") @DefaultValue("91") double latitude,
                                   @FormParam("longitude") @DefaultValue("181") double longitude, @FormParam("since") String since, @FormParam("until") String until, @FormParam("type") String newType,
                                   @FormParam("radius") @DefaultValue("-1") double radius){
        try{
            Guest guest = AuthHelper.getGuest();
            int index = Integer.parseInt(input);
            return updateAddress(guest.username,index,address,latitude,longitude,radius,since,until,newType);

        } catch (Exception e){
            try{
                return addAddress(input,address,latitude,longitude,radius,since,until, AuthHelper.getGuest().username);
            }
            catch (Exception e1){
                StatusModel result = new StatusModel(false, "Could not add/update guest addresses: " + e1.getMessage());
                return gson.toJson(result);
            }
        }
    }

    @DELETE
    @Path("/all")
    @Produces({MediaType.APPLICATION_JSON})
    public String deleteAllAddresses(){
        StatusModel result;
        try{
            Guest guest = AuthHelper.getGuest();
            settingsService.deleteAllAddresses(guest.getId());
            result = new StatusModel(true, "Successfully deleted all addresses");
        } catch (Exception e) {
            result = new StatusModel(false, "Failed to delete all addresses: " + e.getMessage());
        }
        return gson.toJson(result);
    }

    @DELETE
    @Path("/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String deleteAddress(@PathParam("index") int index){
        StatusModel result;
        try{
            Guest guest = AuthHelper.getGuest();
            settingsService.deleteAddressById(guest.getId(),settingsService.getAllAddresses(guest.getId()).get(index).id);
            result = new StatusModel(true, "Successfully deleted address");
        } catch (Exception e) {
            result = new StatusModel(false, "Failed to delete address: " + e.getMessage());
        }
        return gson.toJson(result);
    }

    @DELETE
    @Path("/id/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String deleteAddressById(@PathParam("index") int id){
        StatusModel result;
        try{
            Guest guest = AuthHelper.getGuest();
            settingsService.deleteAddressById(guest.getId(),id);
            result = new StatusModel(true, "Successfully deleted address");
        } catch (Exception e) {
            result = new StatusModel(false, "Failed to delete address: " + e.getMessage());
        }
        return gson.toJson(result);
    }

    @DELETE
    @Path("/{selector}/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String deleteAddressBySingleSelector(@PathParam("selector") String selector, @PathParam("index") int index){
        StatusModel result;
        try{
            Guest guest = AuthHelper.getGuest();
            List<GuestAddress> addresses;
            try{
                addresses = getAddressesAtDate(guest,selector);
            } catch (Exception e) {
                addresses = getAddressesOfType(guest,selector);
            }
            settingsService.deleteAddressById(guest.getId(),addresses.get(index).id);
            result = new StatusModel(true, "Successfully deleted addresses");
        } catch (Exception e) {
            result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());

        }
        return gson.toJson(result);
    }

    @DELETE
    @Path("/{selector}/all")
    @Produces({MediaType.APPLICATION_JSON})
    public String deleteAddressBySingleSelector(@PathParam("selector") String selector){
        StatusModel result;
        try{
            Guest guest = AuthHelper.getGuest();
            try{
                DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), selector);
                settingsService.deleteAllAddressesAtDate(guest.getId(),(dayMeta.start + dayMeta.end) / 2);
            } catch (Exception e) {
                settingsService.deleteAllAddressesOfType(guest.getId(),selector);
            }
            result = new StatusModel(true, "Successfully deleted addresses");
        } catch (Exception e) {
            result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());

        }
        return gson.toJson(result);
    }

    @DELETE
    @Path("/{type}/{date}/all")
    @Produces({MediaType.APPLICATION_JSON})
    public String deleteAllAddressesOfTypeAtDate(@PathParam("type") String type, @PathParam("date") String date){
        StatusModel result;
        try{
            Guest guest = AuthHelper.getGuest();
            DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), date);
            settingsService.deleteAllAddressesOfTypeForDate(guest.getId(),type,(dayMeta.start + dayMeta.end)/2);
            result = new StatusModel(false, "Successfully deleted addresses");
        } catch (Exception e) {
            result = new StatusModel(false, "Could not delete addresses: " + e.getMessage());
        }
        return gson.toJson(result);
    }

    @DELETE
    @Path("/{type}/{date}/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String deleteAddressOfTypeAtDate(@PathParam("type") String type, @PathParam("date") String date, @PathParam("index") int index){
        StatusModel result;
        try{
            Guest guest = AuthHelper.getGuest();
            DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), date);
            List<GuestAddress> addresses = settingsService.getAllAddressesOfTypeForDate(guest.getId(),type,(dayMeta.start + dayMeta.end)/2);
            settingsService.deleteAddressById(guest.getId(),addresses.get(index).id);
            result = new StatusModel(false, "Successfully deleted addresses");
        } catch (Exception e) {
            result = new StatusModel(false, "Could not delete addresses: " + e.getMessage());
        }
        return gson.toJson(result);
    }

    public String updateAddress(String username, int index, String address, double latitude,
                                double longitude, double radius, String since, String until, String newType){
        StatusModel result;
        try{
            Guest guest = guestService.getGuest(username);

            String jsonString = null;
            if (address != null){
                String addressEncoded = URLEncoder.encode(address, "UTF-8");
                jsonString = HttpUtils.fetch("https://maps.googleapis.com/maps/api/geocode/json?sensor=false&address=" + addressEncoded);
            }

            Long startTime = null, endTime = null;
            if (since != null){
                try{
                    DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), since);
                    startTime = dayMeta.start;
                } catch (Exception e){
                    startTime = Long.parseLong(since);
                }
            }
            if (until != null){
                if (until.equalsIgnoreCase("Present"))
                    endTime = Long.MAX_VALUE;
                else{
                    try{
                        DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), until);
                        endTime = dayMeta.end;
                    } catch (Exception e){
                        endTime = Long.parseLong(until);
                    }
                }
            }

            GuestAddress add = settingsService.getAllAddresses(guest.getId()).get(index);
            GuestAddress updatedAddress = settingsService.updateAddress(guest.getId(),add.id,newType,address,latitude > 90 ? null : latitude,
                                          longitude > 180 ? null : longitude,startTime,endTime, radius < 0 ? null : radius,jsonString);
            result = new StatusModel(true, gson.toJson(updatedAddress));
        } catch (Exception e) {
            result = new StatusModel(false, "Failed to update address: " + e.getMessage());
        }
        return gson.toJson(result);
    }

    @POST
    @Path("/{selector}/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateAddressBySingleSelector(@PathParam("selector") String selector, @PathParam("index") int index){
        StatusModel result;
        try{
            Guest guest = AuthHelper.getGuest();
            List<GuestAddress> addresses;
            try{
                addresses = getAddressesAtDate(guest,selector);
            } catch (Exception e) {
                addresses = getAddressesOfType(guest,selector);
            }
            settingsService.deleteAddressById(guest.getId(),addresses.get(index).id);
            result = new StatusModel(true, "Successfully deleted addresses");
        } catch (Exception e) {
            result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());

        }
        return gson.toJson(result);
    }

    @POST
    @Path("/{type}/{date}/{index}")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateAddressOfTypeAtDate(@PathParam("type") String type, @PathParam("date") String date, @PathParam("index") int index){
        StatusModel result;
        try{
            Guest guest = AuthHelper.getGuest();
            DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), date);
            List<GuestAddress> addresses = settingsService.getAllAddressesOfTypeForDate(guest.getId(),type,(dayMeta.start + dayMeta.end)/2);
            settingsService.deleteAddressById(guest.getId(),addresses.get(index).id);
            result = new StatusModel(false, "Successfully deleted addresses");
        } catch (Exception e) {
            result = new StatusModel(false, "Could not delete addresses: " + e.getMessage());
        }
        return gson.toJson(result);
    }
}
