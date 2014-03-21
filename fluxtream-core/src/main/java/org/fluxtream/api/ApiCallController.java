package org.fluxtream.api;

import java.util.TimeZone;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.fluxtream.Configuration;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.SignpostOAuthHelper;
import org.fluxtream.connectors.updaters.RateLimitReachedException;
import org.fluxtream.connectors.updaters.UnexpectedResponseCodeException;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.Guest;
import org.fluxtream.services.BodyTrackStorageService;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.JPADaoService;
import org.fluxtream.utils.TimeUtils;
import com.google.gson.Gson;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/apiCall")
@Component("RESTApiCallController")
@Scope("request")
public class ApiCallController {

	@Autowired
	GuestService guestService;

    @Autowired
    SignpostOAuthHelper signpostHelper;

    @Qualifier("bodyTrackStorageServiceImpl")
    @Autowired
    BodyTrackStorageService bodyTrackStorageService;

    @Autowired
    JPADaoService jpaDaoService;

    Gson gson = new Gson();

	@Autowired
	Configuration env;

    @GET
    @Path("/fitbit/body/weight")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getFitbitBodyWeightHistory() {
        String formattedDate = TimeUtils.dateFormatter.withZone(
                DateTimeZone.forTimeZone(TimeZone.getDefault())).print(System.currentTimeMillis());

        final Connector fitbitConnector = Connector.getConnector("fitbit");
        Guest guest = AuthHelper.getGuest();
        final ApiKey apiKey = guestService.getApiKey(guest.getId(), fitbitConnector);

        try {
            String json = signpostHelper.makeRestCall(apiKey, 8, "http://api.fitbit.com/1/user/-/body/weight/date/" + formattedDate + "/max.json");
            return json;
        }
        catch (RateLimitReachedException e) {
            System.out.println(e.getMessage());
        }
        catch (UnexpectedResponseCodeException e) {
            System.out.println(e.getMessage());
        }
        return "{\"message\":\"error\"}";
    }

    @GET
    @Path("/fitbit/weight")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getFitbitAriaWeightHistory() {
        String formattedDate = TimeUtils.dateFormatter.withZone(
                DateTimeZone.forTimeZone(TimeZone.getDefault())).print(System.currentTimeMillis());

        final Connector fitbitConnector = Connector.getConnector("fitbit");
        Guest guest = AuthHelper.getGuest();
        final ApiKey apiKey = guestService.getApiKey(guest.getId(), fitbitConnector);

        try {
            String json = signpostHelper.makeRestCall(apiKey, 8, "http://api.fitbit.com/1/user/-/body/log/weight/date/" + formattedDate + "/1m.json");
            return json;
        }
        catch (RateLimitReachedException e) {
            System.out.println(e.getMessage());
        }
        catch (UnexpectedResponseCodeException e) {
            System.out.println(e.getMessage());
        }
        return "{\"message\":\"error\"}";
    }
}