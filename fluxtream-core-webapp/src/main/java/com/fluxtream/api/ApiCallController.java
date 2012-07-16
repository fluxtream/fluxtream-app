package com.fluxtream.api;

import java.io.IOException;
import java.util.ArrayList;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.SignpostOAuthHelper;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.JPADaoService;
import com.google.gson.Gson;
import net.sf.json.JSONObject;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
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
	
	@POST
	@Path("/bodymedia/getRegistrationDate")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getRegistrationDate(@QueryParam("username") String username) {
		Guest guest = guestService.getGuest(username);
        final ApiKey apiKey = guestService.getApiKey(guest.getId(), Connector.getConnector("bodymedia"));
        OAuthConsumer consumer = setupConsumer(apiKey);
        String api_key = env.get("bodymediaConsumerKey");
        return getUserRegistrationDate(api_key, consumer);

    }

    @POST
    @Path("/bodytrackHandler")
    @Produces({ MediaType.APPLICATION_JSON })
    public String handleZeo(@QueryParam("username") String username, @QueryParam("connector") String conn) {
        Guest guest = guestService.getGuest(username);
        AbstractFacet facet = jpaDaoService.findOne("bodymedia." + conn + ".between", AbstractFacet.class, guest.getId(), new Long(0), System.currentTimeMillis());
        ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        facets.add(facet);
        bodyTrackStorageService.storeApiData(guest.getId(), facets);
        return null;
    }

    OAuthConsumer setupConsumer(ApiKey apiKey)
    {
        String api_key = env.get("bodymediaConsumerKey");
        String bodymediaConsumerSecret = env.get("bodymediaConsumerSecret");

        OAuthConsumer consumer =
                new CommonsHttpOAuthConsumer(
                api_key,
                bodymediaConsumerSecret);

        String accessToken = apiKey.getAttributeValue("accessToken", env);
        String tokenSecret = apiKey.getAttributeValue("tokenSecret", env);

        consumer.setTokenWithSecret(accessToken,
                                    tokenSecret);
        return consumer;
    }

    public String getUserRegistrationDate(String api_key, OAuthConsumer consumer)
    {
        String requestUrl = "http://api.bodymedia.com/v2/json/user/info?api_key=" + api_key;

        HttpGet request = new HttpGet(requestUrl);
        try {
            consumer.sign(request);
        }
        catch (OAuthMessageSignerException e){
            return "Consumer Signing Failed";
        }
        catch (OAuthExpectationFailedException e) {
            return "Consumer Signing Failed";
        }
        catch (OAuthCommunicationException e) {
            return "Consumer Signing Failed";
        }
        HttpClient client = env.getHttpClient();
        HttpResponse response;
        try {
            response = client.execute(request);
        }
        catch (IOException e) {
            return "Response Execution Failed";
        }
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200)
        {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String json;
            try {
                json = responseHandler.handleResponse(response);
            }
            catch (IOException e) {
                return "Response Handler Failed";
            }
            JSONObject userInfo = JSONObject.fromObject(json);
            return userInfo.getString("registrationDate");
        }
        return "Error: " + statusCode;
    }

}