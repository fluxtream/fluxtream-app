package org.fluxtream.core.api;

import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.oauth2.Application;
import org.fluxtream.core.mvc.models.ApplicationModel;
import org.fluxtream.core.services.PartnerAppsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD operations for Partner Applications. This API resource needs to live inside
 * of the dev webapp because it uses a separate security context.
 */
@Path("/v1/apps")
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
    public Response updateApplication(ApplicationModel appModel,
                                         @PathParam("uid") String uid) {
        return createOrUpdateApplication(appModel, uid);
    }


    @POST
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApplication(ApplicationModel appModel) {
        return createOrUpdateApplication(appModel, null);
    }

    public Response createOrUpdateApplication(ApplicationModel appModel, String uid) {
        final long guestId = AuthHelper.getGuestId();
        try {
            if (uid==null) {
                partnerAppsService.createApplication(guestId, appModel.organization, appModel.name, appModel.description, appModel.website);
                return Response.ok(String.format("Created app '%s': %s", appModel.name, appModel.description)).build();
            } else {
                partnerAppsService.updateApplication(guestId, appModel.organization, appModel.uid, appModel.name, appModel.description, appModel.website);
                return Response.ok(String.format("Updated app '%s': %s", appModel.name, appModel.description)).build();
            }
        }
        catch (Throwable e) {
            return Response.serverError().entity(String.format("Could not create app '%s'", appModel.name)).build();
        }
    }

    @GET
    @Path("/{uid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getApplication(@PathParam("uid") String uid) throws IOException {
        final long guestId = AuthHelper.getGuestId();
        try {
            final Application app = partnerAppsService.getApplication(guestId, uid);
            if (app!=null) {
                return Response.ok(mapper.writeValueAsString(new ApplicationModel(app))).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("No such application: " + uid).build();
            }
        }
        catch (Throwable e) {
            return Response.serverError().entity("Couldn't list applications for user " + guestId).build();
        }
    }

    @DELETE
    @Path("/{uid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteApplication(@PathParam("uid") String uid) {
        final long guestId = AuthHelper.getGuestId();
        try {
            partnerAppsService.deleteApplication(guestId, uid);
            return Response.ok(String.format("Deleted app '%s'", uid)).build();
        }
        catch (Throwable e) {
            return Response.serverError().entity(String.format("Could not delete app '%s'", uid)).build();
        }
    }

    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response listApplications() throws IOException {
        final long guestId = AuthHelper.getGuestId();
        try {
            final List<Application> applications = partnerAppsService.getApplications(guestId);
            List<ApplicationModel> apps = new ArrayList<ApplicationModel>();
            for (Application application : applications) {
                ApplicationModel app = new ApplicationModel(application);
                apps.add(app);
            }
            final String json = mapper.writeValueAsString(apps);
            return Response.ok(json).build();
        }
        catch (Throwable e) {
            return Response.serverError().entity("Couldn't list applications for user " + guestId).build();
        }
    }

}
