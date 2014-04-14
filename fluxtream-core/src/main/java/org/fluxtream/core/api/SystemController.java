package org.fluxtream.core.api;

import javax.ws.rs.Path;
import org.fluxtream.core.api.gson.UpdateInfoSerializer;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.SystemService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.fluxtream.core.aspects.FlxLogger;
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
