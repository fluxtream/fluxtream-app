package com.fluxtream.api;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fluxtream.Configuration;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.GuestService;
import com.google.gson.Gson;

import static com.newrelic.api.agent.NewRelic.setTransactionName;

@Path("/bodytrack")
@Component("RESTBodytrackController")
@Scope("request")
public class BodyTrackController {

	@Autowired
	GuestService guestService;
	
	@Autowired
	BodyTrackStorageService bodytrackStorageService;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

	Gson gson = new Gson();

	@Autowired
	Configuration env;
	
	@POST
	@Path("/uploadHistory")
	@Produces({ MediaType.APPLICATION_JSON })
	public String loadHistory(@QueryParam("username") String username,
			@QueryParam("connectorName") String connectorName) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
        setTransactionName(null, "POST /bodytrack/uploadHistory");
        StatusModel status;
        try{
            Guest guest = guestService.getGuest(username);

            if (!checkForPermissionAccess(guest.getId())){
                status = new StatusModel(false, "Failure!");
            }
            else{
                bodytrackStorageService.storeInitialHistory(guest.getId(), connectorName);
                status = new StatusModel(true, "Success!");
            }
        }
        catch (Exception e){
            status = new StatusModel(false,"Failure!");
        }
        return gson.toJson(status);
    }

    @DELETE
    @Path("/users/{UID}/views/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteBodytrackView(@PathParam("UID") Long uid, @PathParam("id") long viewId){
        setTransactionName(null, "DELETE /bodytrack/users/{UID}/views/{id}");
        StatusModel status;
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            bodyTrackHelper.deleteView(uid, viewId);
            status = new StatusModel(true,"successfully deleted view " + viewId);
        }
        catch (Exception e){
            status = new StatusModel(false,"failed to delete view " + viewId);
        }
        return gson.toJson(status);
    }

    @POST
    @Path("/upload")
    @Consumes({MediaType.MULTIPART_FORM_DATA,MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.APPLICATION_JSON})
    public String uploadToBodytrack(@FormParam("dev_nickname") String deviceNickanme, @FormParam("channel_names") String channels,
                                    @FormParam("data") String data){
        setTransactionName(null, "POST /bodytrack/upload");
        StatusModel status;
        try{
            long uid = ControllerHelper.getGuestId();
            Type channelsType =  new TypeToken<Collection<String>>(){}.getType();
            Type dataType = new TypeToken<List<List<Long>>>(){}.getType();
            bodyTrackHelper.uploadToBodyTrack(uid, deviceNickanme, (Collection<String>)gson.fromJson(channels, channelsType), (List<List<Object>>)gson.fromJson(data, dataType));
            status = new StatusModel(true,"Upload successful!");
        }
        catch (Exception e){
            status = new StatusModel(false,"Upload failed!");
        }
        return gson.toJson(status);
    }

    @GET
    @Path("/tiles/{UID}/{DeviceNickname}.{ChannelName}/{Level}.{Offset}.json")
    @Produces({MediaType.APPLICATION_JSON})
    public String fetchTile(@PathParam("UID") Long uid, @PathParam("DeviceNickname") String deviceNickname,
                                   @PathParam("ChannelName") String channelName, @PathParam("Level") int level, @PathParam("Offset") long offset){
        setTransactionName(null, "GET /bodytrack/tiles/{UID}/" + deviceNickname + "." + channelName + "/{Level}.{Offset}.json");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            return bodyTrackHelper.fetchTile(uid, deviceNickname, channelName, level, offset);
        } catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path("/users/{UID}/views")
    @Produces({MediaType.APPLICATION_JSON})
    public String getViews(@PathParam("UID") Long uid) {
        setTransactionName(null, "GET /bodytrack/users/{UID}/views");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            return bodyTrackHelper.listViews(uid);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path("/users/{UID}/views/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public String bodyTrackView(@PathParam("UID") Long uid, @PathParam("id") long id) {
        setTransactionName(null, "GET /bodytrack/users/{UID}/views/{id}");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            String result = bodyTrackHelper.getView(uid,id);
            return result == null ? gson.toJson(new StatusModel(false,"Failed to get view")) : result;
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @POST
    @Path("/users/{UID}/views")
    @Produces({MediaType.APPLICATION_JSON})
    public String setView(@PathParam("UID") Long uid, @FormParam("name") String name, @FormParam("data") String data) {
        setTransactionName(null, "POST /bodytrack/users/{UID}/views");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            return bodyTrackHelper.saveView(uid,name,data);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path("/users/{UID}/sources/list")
    @Produces({MediaType.APPLICATION_JSON})
    public String getSourceList(@PathParam("UID") Long uid) {
        setTransactionName(null, "GET /bodytrack/users/{UID}/sources/list");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            return bodyTrackHelper.listSources(uid);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path(value = "/users/{UID}/sources/{source}/default_graph_specs")
    @Produces({MediaType.APPLICATION_JSON})
    public String bodyTrackGetDefaultGraphSpecs(@PathParam("UID") Long uid, @PathParam("source") String name) {
        setTransactionName(null, "GET /bodytrack/users/{UID}/sources/{source}/default_graph_specs");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            return bodyTrackHelper.getSourceInfo(uid,name);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @POST
    @Path("/users/{UID}/channels/{DeviceNickname}.{ChannelName}/set")
    @Produces({MediaType.APPLICATION_JSON})
    public String setDefaultStyle(@PathParam("UID") Long uid, @PathParam("DeviceNickname") String deviceNickname,
                                @PathParam("ChannelName") String channelName, @FormParam("user_default_style") String style) {
        setTransactionName(null, "POST /users/{UID}/channels/" + deviceNickname + "." + channelName + "/set");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            bodyTrackHelper.setDefaultStyle(uid,deviceNickname,channelName,style);
            return gson.toJson(new StatusModel(true,"Channel style set"));
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    private boolean checkForPermissionAccess(long targetUid){
        Guest guest = ControllerHelper.getGuest();
        return targetUid == guest.getId() || guest.hasRole(Guest.ROLE_ADMIN) || guest.hasRole(Guest.ROLE_ADMIN);
    }

}
