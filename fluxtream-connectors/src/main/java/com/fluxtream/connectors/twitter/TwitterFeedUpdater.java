	package com.fluxtream.connectors.twitter;

import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;


@Component
@Updater(prettyName = "Twitter", value = 12,
updateStrategyType=com.fluxtream.connectors.Connector.UpdateStrategyType.INCREMENTAL,
objectTypes={TweetFacet.class,
	TwitterDirectMessageFacet.class, TwitterMentionFacet.class})
@JsonFacetCollection(TwitterFacetVOCollection.class)
public class TwitterFeedUpdater extends AbstractUpdater {

	Logger logger = Logger.getLogger(TwitterFeedUpdater.class);
	
	public TwitterFeedUpdater() {
		super();
	}

	public void getScreenName(long guestId) throws Exception {
		HttpGet request = new HttpGet("http://api.twitter.com/1/account/verify_credentials.json");
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() == 200) {
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String json = responseHandler.handleResponse(response);
			JSONObject profile = JSONObject.fromObject(json);
			String screen_name = profile.getString("screen_name");
			guestService.setApiKeyAttribute(guestId, connector(), "screen_name", screen_name);
		}
	}

	OAuthConsumer consumer;

	void setupConsumer(ApiKey apiKey) {
		String twitterConsumerKey = env.get("twitterConsumerKey");
		String twitterConsumerSecret = env.get("twitterConsumerSecret");

		consumer = new CommonsHttpOAuthConsumer(
				twitterConsumerKey,
				twitterConsumerSecret);

		String accessToken = apiKey.getAttributeValue("accessToken", env);
		String tokenSecret = apiKey.getAttributeValue("tokenSecret", env);
		
		consumer.setTokenWithSecret(accessToken,
				tokenSecret);
	}

	@Override
	public void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
		setupConsumer(updateInfo.apiKey);
		
		if (guestService.getApiKeyAttribute(updateInfo.apiKey.getGuestId(),connector(),"screen_name")==null)
			getScreenName(updateInfo.apiKey.getGuestId());
		
		String screen_name = guestService.getApiKeyAttribute(updateInfo.apiKey.getGuestId(),connector(),"screen_name");
		
		List<ObjectType> objectTypes = updateInfo.objectTypes();
		if (objectTypes.contains(ObjectType.getObjectType(connector(), "tweet"))) {
			getStatuses(updateInfo, screen_name);
		} else if (objectTypes.contains(ObjectType.getObjectType(connector(), "mention"))) {
			getMentions(updateInfo);
		} else if (objectTypes.contains(ObjectType.getObjectType(connector(), "dm"))) {
			getReceivedDirectMessages(updateInfo);
			getSentDirectMessages(updateInfo);
		}
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		setupConsumer(updateInfo.apiKey);
		String screen_name = guestService.getApiKeyAttribute(updateInfo.apiKey.getGuestId(),connector(),"screen_name");
		
		List<ObjectType> objectTypes = updateInfo.objectTypes();
		if (objectTypes.contains(ObjectType.getObjectType(connector(), "tweet"))) {
			refreshStatuses(updateInfo, screen_name);
		} else if (objectTypes.contains(ObjectType.getObjectType(connector(), "mention"))) {
			refreshMentions(updateInfo);
		} else if (objectTypes.contains(ObjectType.getObjectType(connector(), "dm"))) {
			refreshReceivedDirectMessages(updateInfo);
			refreshSentDirectMessages(updateInfo);
		}
	}

	private void refreshStatuses(UpdateInfo updateInfo, String screen_name) throws Exception {
		TweetFacet mostRecentTweet = jpaDaoService.findOne("twitter.tweet.newest", TweetFacet.class, updateInfo.apiKey.getGuestId());
		if (mostRecentTweet!=null) {
			int newerTweets = 1;
			while (newerTweets>0) {
				newerTweets = getStatusesAfter(updateInfo, screen_name, mostRecentTweet.tweetId+1);
				mostRecentTweet = jpaDaoService.findOne("twitter.tweet.newest", TweetFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void refreshMentions(UpdateInfo updateInfo) throws Exception {
		TwitterMentionFacet mostRecentMention = jpaDaoService.findOne("twitter.mention.newest", TwitterMentionFacet.class, updateInfo.apiKey.getGuestId());
		if (mostRecentMention!=null) {
			int newerMentions = 1;
			while (newerMentions>0) {
				newerMentions = getMentionsAfter(updateInfo, mostRecentMention.twitterId+1);
				mostRecentMention = jpaDaoService.findOne("twitter.mention.newest", TwitterMentionFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void refreshReceivedDirectMessages(UpdateInfo updateInfo) throws Exception {
		TwitterDirectMessageFacet mostRecentDM = jpaDaoService.findOne("twitter.received.dm.newest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
		if (mostRecentDM!=null) {
			int newerDMs = 1;
			while (newerDMs>0) {
				newerDMs = getDirectMessagesReceivedAfter(updateInfo, mostRecentDM.twitterId+1);
				mostRecentDM = jpaDaoService.findOne("twitter.received.dm.newest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void refreshSentDirectMessages(UpdateInfo updateInfo) throws Exception {
		TwitterDirectMessageFacet mostRecentDM = jpaDaoService.findOne("twitter.sent.dm.newest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
		if (mostRecentDM!=null) {
			int newerDMs = 1;
			while (newerDMs>0) {
				newerDMs = getDirectMessagesSentAfter(updateInfo, mostRecentDM.twitterId+1);
				mostRecentDM = jpaDaoService.findOne("twitter.sent.dm.newest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void getStatuses(UpdateInfo updateInfo, String screen_name) throws Exception {
		getStatuses(updateInfo, screen_name, -1, -1);
		TweetFacet oldestTweet = jpaDaoService.findOne("twitter.tweet.oldest", TweetFacet.class, updateInfo.apiKey.getGuestId());
		if (oldestTweet!=null) {
			int olderTweets = 1;
			while(olderTweets>0) {
				olderTweets = getStatusesBefore(updateInfo, screen_name, oldestTweet.tweetId-1);
				oldestTweet = jpaDaoService.findOne("twitter.tweet.oldest", TweetFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void getMentions(UpdateInfo updateInfo) throws Exception {
		getMentions(updateInfo, -1, -1);
		TwitterMentionFacet oldestMention = jpaDaoService.findOne("twitter.mention.oldest", TwitterMentionFacet.class, updateInfo.apiKey.getGuestId());
		if (oldestMention!=null) {
			int olderMentions = 1;
			while(olderMentions>0) {
				olderMentions = getMentionsBefore(updateInfo, oldestMention.twitterId-1);
				oldestMention = jpaDaoService.findOne("twitter.mention.oldest", TwitterMentionFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void getReceivedDirectMessages(UpdateInfo updateInfo) throws Exception {
		getReceivedDirectMessages(updateInfo, -1, -1);
		TwitterDirectMessageFacet oldestDM = jpaDaoService.findOne("twitter.received.dm.oldest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
		if (oldestDM!=null) {
			int olderDMs = 1;
			while(olderDMs>0) {
				olderDMs = getDirectMessagesReceivedBefore(updateInfo, oldestDM.twitterId-1);
				oldestDM = jpaDaoService.findOne("twitter.received.dm.oldest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void getSentDirectMessages(UpdateInfo updateInfo) throws Exception {
		getSentDirectMessages(updateInfo, -1, -1);
		TwitterDirectMessageFacet oldestDM = jpaDaoService.findOne("twitter.sent.dm.oldest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
		if (oldestDM!=null) {
			int olderDMs = 1;
			while(olderDMs>0) {
				olderDMs = getDirectMessagesSentBefore(updateInfo, oldestDM.twitterId-1);
				oldestDM = jpaDaoService.findOne("twitter.sent.dm.oldest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private int getMentionsBefore(UpdateInfo updateInfo, long max_id) throws Exception {
		return getMentions(updateInfo, max_id, -1);
	}

	private int getMentionsAfter(UpdateInfo updateInfo, long since_id) throws Exception {
		return getMentions(updateInfo, -1, since_id);
	}

	private int getDirectMessagesReceivedBefore(UpdateInfo updateInfo, long max_id) throws Exception {
		return getReceivedDirectMessages(updateInfo, max_id, -1);
	}

	private int getDirectMessagesReceivedAfter(UpdateInfo updateInfo, long since_id) throws Exception {
		return getReceivedDirectMessages(updateInfo, -1, since_id);
	}

	private int getDirectMessagesSentBefore(UpdateInfo updateInfo, long max_id) throws Exception {
		return getSentDirectMessages(updateInfo, max_id, -1);
	}

	private int getDirectMessagesSentAfter(UpdateInfo updateInfo, long since_id) throws Exception {
		return getSentDirectMessages(updateInfo, -1, since_id);
	}

	private int getStatusesBefore(UpdateInfo updateInfo, String screen_name, long max_id) throws Exception {
		return getStatuses(updateInfo, screen_name, max_id, -1);
	}

	private int getStatusesAfter(UpdateInfo updateInfo, String screen_name, long since_id) throws Exception {
		return getStatuses(updateInfo, screen_name, -1, since_id);
	}

	private int getStatuses(UpdateInfo updateInfo, String screen_name, long max_id, long since_id) throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "http://api.twitter.com/1/statuses/user_timeline.json?" +
				"screen_name=" + screen_name + "&exclude_replies=t&count=200";
		if (max_id!=-1)
			requestUrl+="&max_id=" + max_id;
		else if (since_id!=-1)
			requestUrl+="&since_id=" + since_id;

		HttpGet request = new HttpGet(requestUrl);
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() == 200) {
			countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String json = responseHandler.handleResponse(response);
			JSONArray statuses = JSONArray.fromObject(json);
			if (statuses!=null) {
				apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
			}
			return statuses.size();
		} else {
			countFailedApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			throw new Exception("Unexpected error trying to get statuses");
		}
	}

	private int getReceivedDirectMessages(UpdateInfo updateInfo, long max_id, long since_id) throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "http://api.twitter.com/1/direct_messages.json?count=50";
		if (max_id!=-1)
			requestUrl+="&max_id=" + max_id;
		else if (since_id!=-1)
			requestUrl+="&since_id=" + since_id;

		HttpGet request = new HttpGet(requestUrl);
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() == 200) {
			countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String json = responseHandler.handleResponse(response);
			JSONArray directMessages = JSONArray.fromObject(json);
			if (directMessages!=null) {
				updateInfo.setContext("sent", "0");
				apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
			}
			return directMessages.size();
		} else {
			countFailedApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			throw new Exception("Unexpected error trying to get received messages");
		}
	}

	private int getSentDirectMessages(UpdateInfo updateInfo, long max_id, long since_id) throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "http://api.twitter.com/1/direct_messages/sent.json?count=50";
		if (max_id!=-1)
			requestUrl+="&max_id=" + max_id;
		else if (since_id!=-1)
			requestUrl+="&since_id=" + since_id;

		HttpGet request = new HttpGet(requestUrl);
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() == 200) {
			countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String json = responseHandler.handleResponse(response);
			JSONArray directMessages = JSONArray.fromObject(json);
			if (directMessages!=null) {
				updateInfo.setContext("sent", "1");
				apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
			}
			return directMessages.size();
		} else {
			countFailedApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			throw new Exception("Unexpected error trying to get sent messages");
		}
	}

	private int getMentions(UpdateInfo updateInfo, long max_id, long since_id) throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "http://api.twitter.com/1/statuses/mentions.json?count=200";
		if (max_id!=-1)
			requestUrl+="&max_id=" + max_id;
		else if (since_id!=-1)
			requestUrl+="&since_id=" + since_id;

		HttpGet request = new HttpGet(requestUrl);
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() == 200) {
			countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String json = responseHandler.handleResponse(response);
			JSONArray mentions = JSONArray.fromObject(json);
			if (mentions!=null) {
				apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
			}
			return mentions.size();
		} else {
			countFailedApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			Exception exception = new Exception("Unexpected error trying to get mentions: " + response.getStatusLine().getReasonPhrase());
			logger.warn("guestId=" + updateInfo.apiKey.getGuestId() + " action=update connectorName=twitter objectType=mentions error");
			throw exception;
		}
	}
}
