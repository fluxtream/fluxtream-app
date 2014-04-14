package org.fluxtream.core.api;

import java.util.TimeZone;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.SignpostOAuthHelper;
import org.fluxtream.core.connectors.updaters.RateLimitReachedException;
import org.fluxtream.core.connectors.updaters.UnexpectedResponseCodeException;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.BodyTrackStorageService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.utils.TimeUtils;
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