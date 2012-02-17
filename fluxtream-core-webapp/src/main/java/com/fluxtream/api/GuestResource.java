package com.fluxtream.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.GuestModel;
import com.fluxtream.services.GuestService;
import com.google.gson.Gson;

@Path("/guest")
@Component("guestApi")
@Scope("request")
public class GuestResource {

	@Autowired
	GuestService guestService;

	Gson gson = new Gson();

	@GET
	@Path("/")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getCurrentGuest() throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		long guestId = ControllerHelper.getGuestId();

		Guest guest = guestService.getGuestById(guestId);
		GuestModel guestModel = new GuestModel(guest);

//		NewRelic.setTransactionName(null, "/api/log/all/date");
		return gson.toJson(guestModel);
	}

	
}
