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
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD operations for Partner Applications. This API resource needs to live inside
 * of the dev webapp because it uses a separate security context.
 */
@Path("/apps")
@Component("RESTPartnerAppsStore")
@Scope("request")
public class PartnerAppsStore {

    @Autowired
    PartnerAppsService partnerAppsService;

    private final ObjectMapper mapper = new ObjectMapper();

    @POST
    @Path("/{uid}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_JSON)
    public StatusModel updateApplication(ApplicationModel appModel,
                                         @PathParam("uid") String uid) {
        return createOrUpdateApplication(appModel, uid);
    }


    @POST
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_JSON)
    public StatusModel createApplication(ApplicationModel appModel) {
        return createOrUpdateApplication(appModel, null);
    }

    public StatusModel createOrUpdateApplication(ApplicationModel appModel, String uid) {
        final long guestId = AuthHelper.getGuestId();
        try {
            StatusModel status;
            if (uid==null) {
                partnerAppsService.createApplication(guestId, appModel.name, appModel.description, appModel.website);
                status = new StatusModel(true, String.format("Created app '%s': %s", appModel.name, appModel.description));
            } else {
                partnerAppsService.updateApplication(guestId, appModel.uid, appModel.name, appModel.description, appModel.website);
                status = new StatusModel(true, String.format("Updated app '%s': %s", appModel.name, appModel.description));
            }
            return status;
        }
        catch (Throwable e) {
            final String stackTrace = ExceptionUtils.getStackTrace(e);
            final StatusModel statusModel = new StatusModel(false, String.format("Could not create app '%s'", appModel.name));
            statusModel.payload = stackTrace;
            return statusModel;
        }
    }

    @GET
    @Path("/{uid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getApplication(@PathParam("uid") String uid) throws IOException {
        final long guestId = AuthHelper.getGuestId();
        try {
            final Application app = partnerAppsService.getApplication(guestId, uid);
            if (app!=null) {
                final String json = mapper.writeValueAsString(new ApplicationModel(app));
                return json;
            } else {
                final StatusModel statusModel = new StatusModel(false, "No such application: " + uid);
                final String json = mapper.writeValueAsString(statusModel);
                return json;
            }
        }
        catch (Throwable e) {
            final String stackTrace = ExceptionUtils.getStackTrace(e);
            final StatusModel statusModel = new StatusModel(false, "Couldn't list applications for user " + guestId);
            statusModel.payload = stackTrace;
            final String json = mapper.writeValueAsString(statusModel);
            return json;
        }
    }

    @DELETE
    @Path("/{uid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public StatusModel deleteApplication(@PathParam("uid") String uid) {
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
    public String listApplications() throws IOException {
        final long guestId = AuthHelper.getGuestId();
        try {
            final List<Application> applications = partnerAppsService.getApplications(guestId);
            List<ApplicationModel> apps = new ArrayList<ApplicationModel>();
            for (Application application : applications) {
                ApplicationModel app = new ApplicationModel(application);
                apps.add(app);
            }
            final String json = mapper.writeValueAsString(apps);
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
