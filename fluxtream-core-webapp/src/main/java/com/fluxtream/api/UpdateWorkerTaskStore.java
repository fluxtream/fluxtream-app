package com.fluxtream.api;

import java.util.Date;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.UpdateWorkerTask;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ConnectorUpdateService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/updateTasks")
@Component("RESTUpdateWorkerTaskStore")
@Scope("request")
public class UpdateWorkerTaskStore {

    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID("UTC"));

    @GET
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getUpdateTasks(@PathParam("connector") String connectorName,
                             @QueryParam("pageSize") int pageSize,
                             @QueryParam("page") int page) {
        long guestId = ControllerHelper.getGuestId();
        final List<UpdateWorkerTask> scheduledUpdates =
                connectorUpdateService.getScheduledUpdateTasks(guestId, Connector.getConnector(connectorName));
        JSONArray array = new JSONArray();
        for (UpdateWorkerTask scheduledUpdate : scheduledUpdates) {
            array.add(toJSON(scheduledUpdate));
        }
        return array.toString();
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
    public String getObjectTypeUpdateTasks(@PathParam("connector") String connectorName,
                                           @PathParam("objectType") String objectTypeName,
                                           @QueryParam("pageSize") int pageSize,
                                           @QueryParam("page") int page) {
        long guestId = ControllerHelper.getGuestId();
        final Connector connector = Connector.getConnector(connectorName);
        final ObjectType objectType = ObjectType.getObjectType(connector, objectTypeName);
        final UpdateWorkerTask scheduledUpdate =
                connectorUpdateService.getScheduledUpdateTask(guestId, connector.getName(), objectType.value());
        return scheduledUpdate!=null?toJSON(scheduledUpdate).toString():"{}";
    }

    @DELETE
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public String deleteUpdateTasks(@PathParam("connector") String connectorName,
                                    @QueryParam("pageSize") int pageSize,
                                    @QueryParam("page") int page) {
        long guestId = ControllerHelper.getGuestId();
        final Connector connector = Connector.getConnector(connectorName);
        connectorUpdateService.deleteScheduledUpdateTasks(guestId, connector);
        StatusModel statusModel = new StatusModel(true, "successfully deleted pending update tasks for " + connectorName);
        return gson.toJson(statusModel);
    }

}
