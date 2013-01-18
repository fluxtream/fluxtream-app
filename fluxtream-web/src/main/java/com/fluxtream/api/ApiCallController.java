package com.fluxtream.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.TimeZone;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.SignpostOAuthHelper;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.JPADaoService;
import com.google.gson.Gson;
import net.sf.json.JSONObject;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpParameters;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter dateFormat = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    @GET
    @Path("/fitbit/body/weight")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getFitbitBodyWeightHistory() {
        String formattedDate = dateFormat.withZone(
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
        return "{\"message\":\"error\"}";
    }

    @GET
    @Path("/fitbit/weight")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getFitbitAriaWeightHistory() {
        String formattedDate = dateFormat.withZone(
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
        return "{\"message\":\"error\"}";
    }
	
	@POST
	@Path("/bodymedia/getRegistrationDate")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getRegistrationDate(@QueryParam("username") String username) {
        try{
            Guest guest = guestService.getGuest(username);
            final ApiKey apiKey = guestService.getApiKey(guest.getId(), Connector.getConnector("bodymedia"));
            OAuthConsumer consumer = setupConsumer(apiKey);
            String api_key = env.get("bodymediaConsumerKey");
            return getUserRegistrationDate(api_key, consumer);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to retrieve registration date: " + e.getMessage()));
        }
    }

    @POST
    @Path("/bodytrackHandler")
    @Produces({ MediaType.APPLICATION_JSON })
    public String handleBodytrack(@QueryParam("username") String username, @QueryParam("connector") String conn) {
        try{
            Guest guest = guestService.getGuest(username);
            AbstractFacet facet = jpaDaoService.findOne("bodymedia." + conn + ".between", AbstractFacet.class, guest.getId(), 0L, System.currentTimeMillis());
            ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
            facets.add(facet);
            bodyTrackStorageService.storeApiData(guest.getId(), facets);
            return null;
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed: " + e.getMessage()));

        }
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

    @POST
    @Path("/bodymediaReg")
    @Produces({MediaType.TEXT_PLAIN})
    public String addBodymedia(@QueryParam("username") String username)
    {
        try{
            Guest g = guestService.getGuest(username);

            String oauthCallback = env.get("homeBaseUrl") + "bodymedia/upgradeToken";

            if (g.getId() != null)
                oauthCallback += "?guestId=" + g.getId();

            String apiKey = env.get("bodymediaConsumerKey");
            OAuthConsumer consumer = new DefaultOAuthConsumer(
                    apiKey,
                    env.get("bodymediaConsumerSecret"));
            HttpParameters additionalParameter = new HttpParameters();
            additionalParameter.put("api_key", apiKey);
            consumer.setAdditionalParameters(additionalParameter);

            HttpClient httpClient = env.getHttpClient();

            OAuthProvider provider = new CommonsHttpOAuthProvider(
                    "https://api.bodymedia.com/oauth/request_token?api_key="+apiKey,
                    "https://api.bodymedia.com/oauth/access_token?api_key="+apiKey,
                    "https://api.bodymedia.com/oauth/authorize?api_key="+apiKey, httpClient);

            String approvalPageUrl;
            try {
                approvalPageUrl = provider.retrieveRequestToken(consumer,
                        oauthCallback);
            }
            catch (OAuthException e) {
                return "RequestToken Failed";
            }

            System.out.println("the token secret is: " + consumer.getTokenSecret());
                approvalPageUrl+="&oauth_api=" + apiKey;
            try {
                approvalPageUrl = URLDecoder.decode(approvalPageUrl, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                return gson.toJson(e);
            }

            return "redirect:" + approvalPageUrl;
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed: " + e.getMessage()));
        }
    }
}