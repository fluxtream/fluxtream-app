package com.fluxtream.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/test")
@Component("RESTTestController")
@Scope("request")
public class TestController {

    @GET
    @Path("/ping")
    @Produces({ MediaType.APPLICATION_JSON })
    public Status testOne() {
        return new Status("this works!");
    }

}
