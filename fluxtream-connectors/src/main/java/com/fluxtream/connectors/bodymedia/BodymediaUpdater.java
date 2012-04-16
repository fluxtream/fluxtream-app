package com.fluxtream.connectors.bodymedia;

import net.sf.json.JSONObject;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.http.HttpParameters;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.springframework.beans.factory.annotation.Autowired;

import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.SignpostOAuthHelper;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;

@Updater(prettyName = "BodyMedia", value = 88, objectTypes = {
		BodymediaBurnFacet.class, BodymediaSleepFacet.class,
		BodymediaStepsFacet.class }, hasFacets = true, additionalParameters={"api_key"})
public class BodymediaUpdater extends AbstractUpdater {

	@Autowired
	SignpostOAuthHelper signpostHelper;
	
	public BodymediaUpdater() {
		super();
	}

	public void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
		ObjectType burnOT = ObjectType.getObjectType(connector(), "burn");
		String userRegistrationDate = getUserRegistrationDate(updateInfo);
		if (updateInfo.objectTypes().contains(burnOT)) {
			retrieveBurnHistory(updateInfo, userRegistrationDate);
		}		
	}

	private void retrieveBurnHistory(UpdateInfo updateInfo,
			String userRegistrationDate) {
		
	}

	OAuthConsumer consumer;
	
	void setupConsumer(ApiKey apiKey) {
		String api_key = env.get("bodymediaConsumerKey");
		String bodymediaConsumerSecret = env.get("bodymediaConsumerSecret");

		consumer = new CommonsHttpOAuthConsumer(
				api_key,
				bodymediaConsumerSecret);
		HttpParameters additionalParameter = new HttpParameters();
		additionalParameter.put("api_key", api_key);
		consumer.setAdditionalParameters(additionalParameter);

		String accessToken = apiKey.getAttributeValue("accessToken", env);
		String tokenSecret = apiKey.getAttributeValue("tokenSecret", env);
		
		consumer.setTokenWithSecret(accessToken,
				tokenSecret);
	}
	
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		
	}
	
	public String getUserRegistrationDate(UpdateInfo updateInfo)
			throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "http://api.bodymedia.com/v2/json/user/info";

		HttpGet request = new HttpGet(requestUrl);
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() == 200) {
			countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String json = responseHandler.handleResponse(response);
			JSONObject userInfo = JSONObject.fromObject(json);
			return userInfo.getString("registrationDate");
		} else {
			countFailedApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			throw new Exception("Unexpected error trying to get statuses");
		}
	}
}
