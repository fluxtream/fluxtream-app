package com.fluxtream.api;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fluxtream.services.DashboardsService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.WidgetsService;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fluxtream.Configuration;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.GuestService;
import com.google.gson.Gson;

@Path("/admin")
@Component("adminController")
@Scope("request")
public class AdminController {

	@Autowired
	GuestService guestService;

	Gson gson = new Gson();

    @Autowired
    JPADaoService jpaDaoService;

	@Autowired
	Configuration env;

    @Autowired
    WidgetsService widgetsService;

    @GET
	@Path("/properties/{propertyName}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getProperty(@PathParam("propertyName") String propertyName)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {

		if (env.get(propertyName) != null) {
			JSONObject property = new JSONObject();
			property.accumulate("name", propertyName).accumulate("value",
					env.get(propertyName));
			return property.toString();
		}

		StatusModel failure = new StatusModel(false, "property not found: "
				+ propertyName);
		return gson.toJson(failure);
	}

    @POST
    @Path("/executeUpdate")
    @Produces({ MediaType.APPLICATION_JSON })
    public String executeUpdate(@FormParam("jpql") String jpql)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {

        try {
            int results = jpaDaoService.execute(jpql);
            StatusModel result = new StatusModel(true, results + " rows affected");
            return gson.toJson(result);
        } catch (Exception e) {
            StatusModel failure = new StatusModel(false, "Could not execute query: " + e.getMessage());
            return gson.toJson(failure);
        }

    }

    @POST
    @Path("/widgets/refresh")
    @Produces({ MediaType.APPLICATION_JSON })
    public String refreshWidgets() {
        widgetsService.refreshWidgets();
        return gson.toJson(new StatusModel(true, "widgets refreshed"));
    }

}
