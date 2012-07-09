package com.fluxtream.api;

import java.util.List;
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
import com.fluxtream.services.ConnectorUpdateService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    @GET
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getUpdateTasks(@PathParam("connector") String connectorName,
                             @QueryParam("pageSize") int pageSize,
                             @QueryParam("page") int page) {
        long guestId = ControllerHelper.getGuestId();
        final UpdateWorkerTask nextScheduledUpdateTask =
                connectorUpdateService.getNextScheduledUpdateTask(guestId, Connector.getConnector(connectorName), -1);
        return nextScheduledUpdateTask!=null?gson.toJson(nextScheduledUpdateTask):"{}";
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
        final UpdateWorkerTask nextScheduledUpdateTask =
                connectorUpdateService.getNextScheduledUpdateTask(guestId, connector, objectType.value());
        return nextScheduledUpdateTask!=null?gson.toJson(nextScheduledUpdateTask):"{}";
    }

}
