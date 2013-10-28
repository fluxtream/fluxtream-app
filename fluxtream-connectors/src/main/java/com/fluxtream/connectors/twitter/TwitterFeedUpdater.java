	package com.fluxtream.connectors.twitter;

    import java.util.HashMap;
    import java.util.List;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
    import com.fluxtream.domain.ChannelMapping;
    import com.fluxtream.services.impl.BodyTrackHelper;
    import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Component;


@Component
@Updater(prettyName = "Twitter", value = 12,
updateStrategyType=com.fluxtream.connectors.Connector.UpdateStrategyType.INCREMENTAL,
bodytrackResponder = TwitterBodytrackResponder.class,
objectTypes={TweetFacet.class,
	TwitterDirectMessageFacet.class, TwitterMentionFacet.class})
@JsonFacetCollection(TwitterFacetVOCollection.class)
public class TwitterFeedUpdater extends AbstractUpdater {

	FlxLogger logger = FlxLogger.getLogger(TwitterFeedUpdater.class);

    @Autowired
    BodyTrackHelper bodyTrackHelper;

	public TwitterFeedUpdater() {
		super();
	}

	public void getScreenName(ApiKey apiKey, OAuthConsumer consumer) throws Exception {
		HttpGet request = new HttpGet("http://api.twitter.com/1.1/account/verify_credentials.json");
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() == 200) {
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String json = responseHandler.handleResponse(response);
			JSONObject profile = JSONObject.fromObject(json);
			String screen_name = profile.getString("screen_name");
			guestService.setApiKeyAttribute(apiKey,  "screen_name", screen_name);
		}
	}

    OAuthConsumer setupConsumer(ApiKey apiKey) {
		String twitterConsumerKey = guestService.getApiKeyAttribute(apiKey, "twitterConsumerKey");
		String twitterConsumerSecret = guestService.getApiKeyAttribute(apiKey, "twitterConsumerSecret");

        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
				twitterConsumerKey,
				twitterConsumerSecret);

		String accessToken = guestService.getApiKeyAttribute(apiKey, "accessToken");
		String tokenSecret = guestService.getApiKeyAttribute(apiKey,"tokenSecret");
		
		consumer.setTokenWithSecret(accessToken,
				tokenSecret);

        return consumer;
	}

    private void initChannelMapping(UpdateInfo updateInfo) {
        List<ChannelMapping> mappings = bodyTrackHelper.getChannelMappings(updateInfo.apiKey);
        if (mappings.size() == 0){
            ChannelMapping mapping = new ChannelMapping();
            mapping.deviceName = "twitter";
            mapping.channelName = "activity";
            mapping.timeType = ChannelMapping.TimeType.gmt;
            mapping.channelType = ChannelMapping.ChannelType.timespan;
            mapping.guestId = updateInfo.getGuestId();
            mapping.apiKeyId = updateInfo.apiKey.getId();
            bodyTrackHelper.persistChannelMapping(mapping);

            BodyTrackHelper.ChannelStyle channelStyle = new BodyTrackHelper.ChannelStyle();
            channelStyle.timespanStyles = new BodyTrackHelper.MainTimespanStyle();
            channelStyle.timespanStyles.defaultStyle = new BodyTrackHelper.TimespanStyle();
            channelStyle.timespanStyles.defaultStyle.fillColor = "#92EF75";
            channelStyle.timespanStyles.defaultStyle.borderColor = "#92EF75";
            channelStyle.timespanStyles.defaultStyle.borderWidth = 2;
            channelStyle.timespanStyles.defaultStyle.top = 0.0;
            channelStyle.timespanStyles.defaultStyle.bottom = 1.0;
            channelStyle.timespanStyles.values = new HashMap();

            BodyTrackHelper.TimespanStyle stylePart = new BodyTrackHelper.TimespanStyle();
            stylePart.top = 0.25;
            stylePart.bottom = 0.75;
            stylePart.fillColor = "#92EF75";
            stylePart.borderColor = "#92EF75";
            channelStyle.timespanStyles.values.put("tweet",stylePart);

            stylePart = new BodyTrackHelper.TimespanStyle();
            stylePart.top = 0.25;
            stylePart.bottom = 0.75;
            stylePart.fillColor = "#92EF75";
            stylePart.borderColor = "#92EF75";
            channelStyle.timespanStyles.values.put("dm",stylePart);

            stylePart = new BodyTrackHelper.TimespanStyle();
            stylePart.top = 0.25;
            stylePart.bottom = 0.75;
            stylePart.fillColor = "#92EF75";
            stylePart.borderColor = "#92EF75";
            channelStyle.timespanStyles.values.put("mention",stylePart);

            bodyTrackHelper.setBuiltinDefaultStyle(updateInfo.getGuestId(), "twitter", "activity", channelStyle);
        }
    }

    @Override
	public void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        initChannelMapping(updateInfo);
        final OAuthConsumer consumer = setupConsumer(updateInfo.apiKey);

        if (guestService.getApiKeyAttribute(updateInfo.apiKey, "screen_name")==null)
			getScreenName(updateInfo.apiKey, consumer);
		
		String screen_name = guestService.getApiKeyAttribute(updateInfo.apiKey, "screen_name");
		
		List<ObjectType> objectTypes = updateInfo.objectTypes();
		if (objectTypes.contains(ObjectType.getObjectType(connector(), "tweet"))) {
			getStatuses(updateInfo, screen_name, consumer);
		} else if (objectTypes.contains(ObjectType.getObjectType(connector(), "mention"))) {
			getMentions(updateInfo, consumer);
		} else if (objectTypes.contains(ObjectType.getObjectType(connector(), "dm"))) {
			getReceivedDirectMessages(updateInfo, consumer);
			getSentDirectMessages(updateInfo, consumer);
		}
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        initChannelMapping(updateInfo);
        final OAuthConsumer consumer = setupConsumer(updateInfo.apiKey);
        String screen_name = guestService.getApiKeyAttribute(updateInfo.apiKey, "screen_name");
		
		List<ObjectType> objectTypes = updateInfo.objectTypes();
		if (objectTypes.contains(ObjectType.getObjectType(connector(), "tweet"))) {
			refreshStatuses(updateInfo, screen_name, consumer);
		} else if (objectTypes.contains(ObjectType.getObjectType(connector(), "mention"))) {
			refreshMentions(updateInfo, consumer);
		} else if (objectTypes.contains(ObjectType.getObjectType(connector(), "dm"))) {
			refreshReceivedDirectMessages(updateInfo, consumer);
			refreshSentDirectMessages(updateInfo, consumer);
		}
	}

	private void refreshStatuses(UpdateInfo updateInfo, String screen_name, OAuthConsumer consumer) throws Exception {
		TweetFacet mostRecentTweet = jpaDaoService.findOne("twitter.tweet.newest", TweetFacet.class, updateInfo.apiKey.getGuestId());
		if (mostRecentTweet!=null) {
			int newerTweets = 1;
			while (newerTweets>0) {
				newerTweets = getStatusesAfter(updateInfo, screen_name, mostRecentTweet.tweetId+1, consumer);
				mostRecentTweet = jpaDaoService.findOne("twitter.tweet.newest", TweetFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void refreshMentions(UpdateInfo updateInfo, OAuthConsumer consumer) throws Exception {
		TwitterMentionFacet mostRecentMention = jpaDaoService.findOne("twitter.mention.newest", TwitterMentionFacet.class, updateInfo.apiKey.getGuestId());
		if (mostRecentMention!=null) {
			int newerMentions = 1;
			while (newerMentions>0) {
				newerMentions = getMentionsAfter(updateInfo, mostRecentMention.twitterId+1, consumer);
				mostRecentMention = jpaDaoService.findOne("twitter.mention.newest", TwitterMentionFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void refreshReceivedDirectMessages(UpdateInfo updateInfo, OAuthConsumer consumer) throws Exception {
		TwitterDirectMessageFacet mostRecentDM = jpaDaoService.findOne("twitter.received.dm.newest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
		if (mostRecentDM!=null) {
			int newerDMs = 1;
			while (newerDMs>0) {
				newerDMs = getDirectMessagesReceivedAfter(updateInfo, mostRecentDM.twitterId+1, consumer);
				mostRecentDM = jpaDaoService.findOne("twitter.received.dm.newest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void refreshSentDirectMessages(UpdateInfo updateInfo, OAuthConsumer consumer) throws Exception {
		TwitterDirectMessageFacet mostRecentDM = jpaDaoService.findOne("twitter.sent.dm.newest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
		if (mostRecentDM!=null) {
			int newerDMs = 1;
			while (newerDMs>0) {
				newerDMs = getDirectMessagesSentAfter(updateInfo, mostRecentDM.twitterId+1, consumer);
				mostRecentDM = jpaDaoService.findOne("twitter.sent.dm.newest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void getStatuses(UpdateInfo updateInfo, String screen_name, OAuthConsumer consumer) throws Exception {
		getStatuses(updateInfo, screen_name, -1, -1, consumer);
		TweetFacet oldestTweet = jpaDaoService.findOne("twitter.tweet.oldest", TweetFacet.class, updateInfo.apiKey.getGuestId());
		if (oldestTweet!=null) {
			int olderTweets = 1;
			while(olderTweets>0) {
				olderTweets = getStatusesBefore(updateInfo, screen_name, oldestTweet.tweetId-1, consumer);
				oldestTweet = jpaDaoService.findOne("twitter.tweet.oldest", TweetFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void getMentions(UpdateInfo updateInfo, OAuthConsumer consumer) throws Exception {
		getMentions(updateInfo, -1, -1, consumer);
		TwitterMentionFacet oldestMention = jpaDaoService.findOne("twitter.mention.oldest", TwitterMentionFacet.class, updateInfo.apiKey.getGuestId());
		if (oldestMention!=null) {
			int olderMentions = 1;
			while(olderMentions>0) {
				olderMentions = getMentionsBefore(updateInfo, oldestMention.twitterId-1, consumer);
				oldestMention = jpaDaoService.findOne("twitter.mention.oldest", TwitterMentionFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void getReceivedDirectMessages(UpdateInfo updateInfo, OAuthConsumer consumer) throws Exception {
		getReceivedDirectMessages(updateInfo, -1, -1, consumer);
		TwitterDirectMessageFacet oldestDM = jpaDaoService.findOne("twitter.received.dm.oldest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
		if (oldestDM!=null) {
			int olderDMs = 1;
			while(olderDMs>0) {
				olderDMs = getDirectMessagesReceivedBefore(updateInfo, oldestDM.twitterId-1, consumer);
				oldestDM = jpaDaoService.findOne("twitter.received.dm.oldest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private void getSentDirectMessages(UpdateInfo updateInfo, OAuthConsumer consumer) throws Exception {
		getSentDirectMessages(updateInfo, -1, -1, consumer);
		TwitterDirectMessageFacet oldestDM = jpaDaoService.findOne("twitter.sent.dm.oldest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
		if (oldestDM!=null) {
			int olderDMs = 1;
			while(olderDMs>0) {
				olderDMs = getDirectMessagesSentBefore(updateInfo, oldestDM.twitterId-1, consumer);
				oldestDM = jpaDaoService.findOne("twitter.sent.dm.oldest", TwitterDirectMessageFacet.class, updateInfo.apiKey.getGuestId());
			}
		}
	}

	private int getMentionsBefore(UpdateInfo updateInfo, long max_id, OAuthConsumer consumer) throws Exception {
		return getMentions(updateInfo, max_id, -1, consumer);
	}

	private int getMentionsAfter(UpdateInfo updateInfo, long since_id, OAuthConsumer consumer) throws Exception {
		return getMentions(updateInfo, -1, since_id, consumer);
	}

	private int getDirectMessagesReceivedBefore(UpdateInfo updateInfo, long max_id, OAuthConsumer consumer) throws Exception {
		return getReceivedDirectMessages(updateInfo, max_id, -1, consumer);
	}

	private int getDirectMessagesReceivedAfter(UpdateInfo updateInfo, long since_id, OAuthConsumer consumer) throws Exception {
		return getReceivedDirectMessages(updateInfo, -1, since_id, consumer);
	}

	private int getDirectMessagesSentBefore(UpdateInfo updateInfo, long max_id, OAuthConsumer consumer) throws Exception {
		return getSentDirectMessages(updateInfo, max_id, -1, consumer);
	}

	private int getDirectMessagesSentAfter(UpdateInfo updateInfo, long since_id, OAuthConsumer consumer) throws Exception {
		return getSentDirectMessages(updateInfo, -1, since_id, consumer);
	}

	private int getStatusesBefore(UpdateInfo updateInfo, String screen_name, long max_id, OAuthConsumer consumer) throws Exception {
		return getStatuses(updateInfo, screen_name, max_id, -1, consumer);
	}

	private int getStatusesAfter(UpdateInfo updateInfo, String screen_name, long since_id, OAuthConsumer consumer) throws Exception {
		return getStatuses(updateInfo, screen_name, -1, since_id, consumer);
	}

	private int getStatuses(UpdateInfo updateInfo, String screen_name, long max_id, long since_id, OAuthConsumer consumer) throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "http://api.twitter.com/1.1/statuses/user_timeline.json?" +
				"screen_name=" + screen_name + "&exclude_replies=t&count=200";
		if (max_id!=-1)
			requestUrl+="&max_id=" + max_id;
		else if (since_id!=-1)
			requestUrl+="&since_id=" + since_id;

		HttpGet request = new HttpGet(requestUrl);
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
			countSuccessfulApiCall(updateInfo.apiKey,
					updateInfo.objectTypes, then, requestUrl);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String json = responseHandler.handleResponse(response);
			JSONArray statuses = JSONArray.fromObject(json);
			if (statuses!=null) {
				apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
			}
			return statuses.size();
		} else {
            final String reasonPhrase = response.getStatusLine().getReasonPhrase();
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, requestUrl, reasonPhrase,
                               statusCode, reasonPhrase);
			throw new Exception("Unexpected error trying to get statuses");
		}
	}

	private int getReceivedDirectMessages(UpdateInfo updateInfo, long max_id, long since_id, OAuthConsumer consumer) throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "http://api.twitter.com/1.1/direct_messages.json?count=200";
		if (max_id!=-1)
			requestUrl+="&max_id=" + max_id;
		else if (since_id!=-1)
			requestUrl+="&since_id=" + since_id;

		HttpGet request = new HttpGet(requestUrl);
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
			countSuccessfulApiCall(updateInfo.apiKey,
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
            final String reasonPhrase = response.getStatusLine().getReasonPhrase();
            logger.warn("getReceivedDirectMessages reason=" +
                        reasonPhrase + " httpCode=" + response.getStatusLine().getStatusCode());
			countFailedApiCall(updateInfo.apiKey,
					updateInfo.objectTypes, then, requestUrl, reasonPhrase,
                    statusCode, reasonPhrase);
			throw new Exception("Unexpected error trying to get received messages");
		}
	}

	private int getSentDirectMessages(UpdateInfo updateInfo, long max_id, long since_id, OAuthConsumer consumer) throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "http://api.twitter.com/1.1/direct_messages/sent.json?count=200";
		if (max_id!=-1)
			requestUrl+="&max_id=" + max_id;
		else if (since_id!=-1)
			requestUrl+="&since_id=" + since_id;

		HttpGet request = new HttpGet(requestUrl);
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
			countSuccessfulApiCall(updateInfo.apiKey,
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
            final String reasonPhrase = response.getStatusLine().getReasonPhrase();
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, requestUrl, reasonPhrase,
                               statusCode, reasonPhrase);
			throw new Exception("Unexpected error trying to get sent messages");
		}
	}

	private int getMentions(UpdateInfo updateInfo, long max_id, long since_id, OAuthConsumer consumer) throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "http://api.twitter.com/1.1/statuses/mentions_timeline.json?count=200";
		if (max_id!=-1)
			requestUrl+="&max_id=" + max_id;
		else if (since_id!=-1)
			requestUrl+="&since_id=" + since_id;

		HttpGet request = new HttpGet(requestUrl);
		consumer.sign(request);
		HttpClient client = env.getHttpClient();
		HttpResponse response = client.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
			countSuccessfulApiCall(updateInfo.apiKey,
					updateInfo.objectTypes, then, requestUrl);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String json = responseHandler.handleResponse(response);
			JSONArray mentions = JSONArray.fromObject(json);
			if (mentions!=null) {
				apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
			}
			return mentions.size();
		} else {
            final String reasonPhrase = response.getStatusLine().getReasonPhrase();
            countFailedApiCall(updateInfo.apiKey,
                               updateInfo.objectTypes, then, requestUrl, reasonPhrase,
                               statusCode, reasonPhrase);
            Exception exception = new Exception("Unexpected error trying to get mentions: " + reasonPhrase);
			throw exception;
		}
	}
}
