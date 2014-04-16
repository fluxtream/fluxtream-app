package org.fluxtream.core.api;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.oauth2.Application;
import org.fluxtream.core.mvc.models.ApplicationModel;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.PartnerAppsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

/**
 * User: candide
 * Date: 16/04/14
 * Time: 10:18
 */
@Path("/apps")
@Component("RESTPartnerAppsStore")
@Scope("request")
public class PartnerAppsStore {

    @Autowired
    PartnerAppsService partnerAppsService;

    private final ObjectMapper mapper = new ObjectMapper();

    @POST
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_JSON)
    public StatusModel createApplication(ApplicationModel appModel) {
        final long guestId = AuthHelper.getGuestId();
        try {
            partnerAppsService.createApplication(guestId, appModel.name, appModel.description);
            StatusModel status = new StatusModel(true, String.format("Created app '%s': %s", appModel.name, appModel.description));
            return status;
        }
        catch (Throwable e) {
            final String stackTrace = ExceptionUtils.getStackTrace(e);
            final StatusModel statusModel = new StatusModel(false, String.format("Could not create app '%s'", appModel.name));
            statusModel.payload = stackTrace;
            return statusModel;
        }
    }

    @DELETE
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    public StatusModel incrementCounter(@FormParam("uid") String uid) {
        final long guestId = AuthHelper.getGuestId();
        try {
            partnerAppsService.deleteApplication(guestId, uid);
            StatusModel status = new StatusModel(true, String.format("Deleted app '%s'", uid));
            return status;
        }
        catch (Throwable e) {
            final String stackTrace = ExceptionUtils.getStackTrace(e);
            final StatusModel statusModel = new StatusModel(false, String.format("Could not delete app '%s'", uid));
            statusModel.payload = stackTrace;
            return statusModel;
        }
    }

    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    public String listApplication() throws IOException {
        final long guestId = AuthHelper.getGuestId();
        try {
            final List<Application> applications = partnerAppsService.getApplications(guestId);
            final String json = mapper.writeValueAsString(applications);
            return json;
        }
        catch (Throwable e) {
            final String stackTrace = ExceptionUtils.getStackTrace(e);
            final StatusModel statusModel = new StatusModel(false, "Couldn't list applications for user " + guestId);
            statusModel.payload = stackTrace;
            final String json = mapper.writeValueAsString(statusModel);
            return json;
        }
    }

}
