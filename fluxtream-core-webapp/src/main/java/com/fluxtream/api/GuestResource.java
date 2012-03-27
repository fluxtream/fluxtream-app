package com.fluxtream.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.mvc.models.guest.GuestModel;
import com.fluxtream.mvc.models.guest.OAuthTokensModel;
import com.fluxtream.services.GuestService;
import com.google.gson.Gson;

@Path("/guest")
@Component("guestApi")
@Scope("request")
public class GuestResource {

	@Autowired
	GuestService guestService;

	Gson gson = new Gson();

	@Autowired
	Configuration env;

	@GET
	@Path("/")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getCurrentGuest() throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		long guestId = ControllerHelper.getGuestId();

		Guest guest = guestService.getGuestById(guestId);
		GuestModel guestModel = new GuestModel(guest);

		// NewRelic.setTransactionName(null, "/api/log/all/date");
		return gson.toJson(guestModel);
	}

	@GET
	@Path("/{connector}/oauthTokens")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getOAuthTokens(@PathParam("connector") String connectorName)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		long guestId = ControllerHelper.getGuestId();

		ApiKey apiKey = guestService.getApiKey(guestId,
				Connector.getConnector(connectorName));
		if (apiKey != null) {
			OAuthTokensModel oauthTokensModel = new OAuthTokensModel();
			oauthTokensModel.accessToken = apiKey.getAttributeValue(
					"accessToken", env);
			oauthTokensModel.tokenSecret = apiKey.getAttributeValue(
					"tokenSecret", env);

			return gson.toJson(oauthTokensModel);
		} else {
			StatusModel result = new StatusModel(false,
					"Guest does not have that connector: " + connectorName);
			return gson.toJson(result);
		}
	}

	@POST
	@Path("/create")
	@Produces({ MediaType.APPLICATION_JSON })
	public String createGuest(@FormParam("username") String username,
			@FormParam("firstname") String firstname,
			@FormParam("lastname") String lastname,
			@FormParam("password") String password,
			@FormParam("email") String email) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		try {
			guestService.createGuest(username, firstname, lastname, password,
					email);
			StatusModel result = new StatusModel(true, "User " + username
					+ " was successfully created");
			return gson.toJson(result);
		} catch (Exception e) {
			StatusModel result = new StatusModel(false,
					"Could not create guest: " + e.getMessage());
			return gson.toJson(result);
		}

	}

	@DELETE
	@Path("/{username}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String deleteGuest(@PathParam("username") String username)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		try {
			guestService.eraseGuestInfo(username);
			StatusModel result = new StatusModel(true, "User " + username
					+ " was successfully deleted");
			return gson.toJson(result);
		} catch (Exception e) {
			StatusModel result = new StatusModel(false,
					"Could not delete guest: " + e.getMessage());
			return gson.toJson(result);
		}

	}

	@GET
	@Path("/all")
	@Produces({ MediaType.APPLICATION_JSON })
	public String list()
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		List<Guest> list = guestService.getAllGuests();
		JSONArray array = new JSONArray();
		for (Guest guest : list) {
			JSONObject guestJson = new JSONObject();
			guestJson.accumulate("username", guest.username)
				.accumulate("firstname", guest.firstname)
				.accumulate("lastname", guest.lastname)
				.accumulate("roles", guest.getUserRoles());
			array.add(guestJson);
		}
		return array.toString();
	}

}
