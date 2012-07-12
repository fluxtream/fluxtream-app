package com.fluxtream.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.ScheduleResult;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.UpdateWorkerTask;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SystemService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/sync")
@Component("RESTSyncController")
@Scope("request")
public class SyncController {

    @Autowired
    GuestService guestService;

    @Autowired
    SystemService sysService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    private ApiDataService apiDataService;

    Gson gson = new Gson();

    @POST
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateConnector(@PathParam("connector") String connectorName){
        final List<ScheduleResult> scheduleResults = connectorUpdateService.updateConnector(ControllerHelper.getGuestId(),
                                                                                            Connector.getConnector(connectorName));
        StatusModel statusModel = new StatusModel(true, "successfully added update worker tasks to the queue (see details)");
        statusModel.payload = scheduleResults;
        return gson.toJson(scheduleResults);
    }

    @POST
    @Path("/{connector}/{objectTypes}")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateConnectorObjectType(@PathParam("connector") String connectorName,
                                            @PathParam("objectTypes") int objectTypes){
        final List<ScheduleResult> scheduleResults = connectorUpdateService.updateConnectorObjectType(ControllerHelper.getGuestId(),
                                                                                            Connector.getConnector(connectorName),
                                                                                            objectTypes);
        StatusModel statusModel = new StatusModel(true, "successfully added update worker tasks to the queue (see details)");
        statusModel.payload = scheduleResults;
        return gson.toJson(scheduleResults);
    }

    @POST
    @Path("/all")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateAllConnectors(){
        final List<ScheduleResult> scheduleResults = connectorUpdateService.updateAllConnectors(ControllerHelper.getGuestId());
        StatusModel statusModel = new StatusModel(true, "successfully added update worker tasks to the queue (see details)");
        statusModel.payload = scheduleResults;
        return gson.toJson(scheduleResults);
    }

}
