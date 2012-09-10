package com.fluxtream.api;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.api.gson.UpdateInfoSerializer;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.SystemService;
import com.fluxtream.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.newrelic.api.agent.NewRelic.setTransactionName;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/system")
@Component("RESTSystemController")
@Scope("request")
public class SystemController {

    Logger logger = Logger.getLogger(SystemController.class);

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    SystemService sysService;

    Gson gson;

    public SystemController() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(UpdateInfo.class, new UpdateInfoSerializer());
        gson = gsonBuilder.create();
    }

    @GET
    @Path("/runningUpdates")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAllRunningUpdates(){
        try {
            final List<UpdateInfo> runningUpdates = connectorUpdateService.getAllRunningUpdates();
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=getAllRunningUpdates");
            logger.info(sb.toString());
            return gson.toJson(runningUpdates);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=getAllRunningUpdates")
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to get all running updates: " + e.getMessage()));
        }
    }

    @POST
    @Path("/shutdown")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel shutdown(@PathParam("connector") String connectorName){
        setTransactionName(null, "POST /system/shutdown");
        long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=systemController action=shutdown")
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        sysService.shutdown();
        return new StatusModel(true, "Service shutdown has been requested");
    }

    @GET
    @Path("/isShutdown")
    @Produces({MediaType.APPLICATION_JSON})
    public String isShutdown(@PathParam("connector") String connectorName){
        setTransactionName(null, "GET /system/isShutdown");
        final boolean shutdown = sysService.isShutdown();
        long guestId = ControllerHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=systemController action=shutdown")
                .append(" guestId=").append(guestId)
                .append(" shutdown=").append(shutdown);
        logger.info(sb.toString());
        return (new StringBuilder("{\"isShutdown\":\"").append(shutdown).append("\"}")).toString();
    }

}
