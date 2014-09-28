package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.UpdateWorkerTask;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/v1/updateTasks")
@Component("RESTUpdateWorkerTaskStore")
@Scope("request")
public class UpdateWorkerTaskStore {

    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
    ConnectorUpdateService connectorUpdateService;

    private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID("UTC"));

    @Autowired
    private GuestService guestService;

    @GET
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUpdateTasks(@PathParam("connector") String connectorName) {
        try{
            long guestId = AuthHelper.getGuestId();

            ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
            final List<UpdateWorkerTask> scheduledUpdates =
                    connectorUpdateService.getScheduledOrInProgressUpdateTasks(apiKey);
            JSONArray array = new JSONArray();
            for (UpdateWorkerTask scheduledUpdate : scheduledUpdates) {
                array.add(toJSON(scheduledUpdate));
            }
            return Response.ok(array.toString()).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to get udpate tasks: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/all")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUpdateTasksAll() {
        try{
            long guestId = AuthHelper.getGuestId();
            final Collection<Connector> connectors = Connector.getAllConnectors();
            JSONArray res = new JSONArray();
            for(Connector c : connectors)
            {
                ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(c.getName()));
                final List<UpdateWorkerTask> scheduledUpdates =
                        connectorUpdateService.getScheduledOrInProgressUpdateTasks(apiKey);
                JSONArray array = new JSONArray();
                for (UpdateWorkerTask scheduledUpdate : scheduledUpdates) {
                    array.add(toJSON(scheduledUpdate));
                }
                JSONObject connectorStatus = new JSONObject();
                connectorStatus.accumulate("name", c.getName());
                connectorStatus.accumulate("status", array);
                res.add(connectorStatus);
            }
            return Response.ok(res.toString()).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to get update tasks: " + e.getMessage()).build();
        }
    }

    private JSONObject toJSON(UpdateWorkerTask task) {
        JSONObject json = new JSONObject();
        json.accumulate("objectTypes", task.getObjectTypes());
        json.accumulate("updateType", task.updateType.toString());
        json.accumulate("timeScheduled", fmt.print(task.timeScheduled));
        json.accumulate("retries", task.retries);
        json.accumulate("status", task.status.toString());
        json.accumulate("jsonParams", task.jsonParams);
        json.accumulate("auditTrail", task.auditTrail);
        return json;
    }

    @GET
    @Path("/{connector}/{objectType}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getObjectTypeUpdateTasks(@PathParam("connector") String connectorName, @PathParam("objectType") String objectTypeName) {
        try{
            long guestId = AuthHelper.getGuestId();
            final Connector connector = Connector.getConnector(connectorName);
            final ObjectType objectType = ObjectType.getObjectType(connector, objectTypeName);
            ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
            final UpdateWorkerTask scheduledUpdate =
                    connectorUpdateService.getUpdateWorkerTask(apiKey, objectType.value());
            return Response.ok(scheduledUpdate!=null?toJSON(scheduledUpdate).toString():"{}").build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to get update tasks: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteUpdateTasks(@PathParam("connector") String connectorName) {
        try{
            long guestId = AuthHelper.getGuestId();
            ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
            connectorUpdateService.flushUpdateWorkerTasks(apiKey, false);
            return Response.ok("successfully deleted pending update tasks for " + connectorName).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to get update tasks: " + e.getMessage()).build();
        }
    }

}
