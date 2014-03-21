package org.fluxtream.api;

import javax.ws.rs.Path;
import org.fluxtream.api.gson.UpdateInfoSerializer;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.services.ConnectorUpdateService;
import org.fluxtream.services.SystemService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.fluxtream.aspects.FlxLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/system")
@Component("RESTSystemController")
@Scope("request")
public class SystemController {

    FlxLogger logger = FlxLogger.getLogger(SystemController.class);

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

}
