package org.fluxtream.api;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.fluxtream.aspects.FlxLogger;

/**
 * Created with IntelliJ IDEA.
 * User: candide
 * Date: 05/03/13
 * Time: 11:50
 * To change this template use File | Settings | File Templates.
 */
@Path("/ping")
public class PingController {

    FlxLogger logger = FlxLogger.getLogger(PingController.class);

    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public String ping() throws IOException {
        return "pong";
    }

}
