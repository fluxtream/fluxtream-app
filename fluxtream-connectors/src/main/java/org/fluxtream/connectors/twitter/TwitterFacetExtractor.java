package org.fluxtream.connectors.twitter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.fluxtream.ApiData;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.facets.extractors.AbstractFacetExtractor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class TwitterFacetExtractor extends AbstractFacetExtractor {
	
	private static final DateTimeFormatter format = DateTimeFormat.forPattern("EEE MMM d HH:mm:ss Z yyyy");
	
	public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData, final ObjectType objectType) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

		JSONArray feed = JSONArray.fromObject(apiData.json);
				
		@SuppressWarnings("rawtypes")
		Iterator iterator = feed.iterator();
		while(iterator.hasNext()) {

			JSONObject twitterItem = (JSONObject)iterator.next();
			
			switch (objectType.value()) {
				case 1:
                {
					TweetFacet tweetFacet = new TweetFacet(apiData.updateInfo.apiKey.getId());
					super.extractCommonFacetData(tweetFacet, apiData);
		
					long createdAtTime = parseDate(twitterItem.getString("created_at"));
					tweetFacet.text = StringEscapeUtils.unescapeHtml(twitterItem.getString("text"));
					tweetFacet.start = createdAtTime;
					tweetFacet.end = createdAtTime;
					tweetFacet.time = createdAtTime;
                    tweetFacet.tweetId = twitterItem.getLong("id");
                    JSONObject user = twitterItem.getJSONObject("user");
					tweetFacet.profileImageUrl = user.getString("profile_image_url");
                    tweetFacet.userName = user.getString("screen_name");
					facets.add(tweetFacet);
					break;
                }
				case 4:
                {
					TwitterMentionFacet twitterMentionFacet = new TwitterMentionFacet(apiData.updateInfo.apiKey.getId());
					super.extractCommonFacetData(twitterMentionFacet, apiData);

                    long createdAtTime = parseDate(twitterItem.getString("created_at"));
					twitterMentionFacet.text =  StringEscapeUtils.unescapeHtml(twitterItem.getString("text"));
					twitterMentionFacet.start = createdAtTime;
					twitterMentionFacet.end = createdAtTime;
					twitterMentionFacet.time = createdAtTime;
					twitterMentionFacet.twitterId = twitterItem.getLong("id");
					JSONObject user = twitterItem.getJSONObject("user");
					twitterMentionFacet.profileImageUrl = user.getString("profile_image_url");
					twitterMentionFacet.userName = user.getString("screen_name");
                    twitterMentionFacet.name = user.getString("name");
					facets.add(twitterMentionFacet);
					break;
                }
				case 2:
                {
					TwitterDirectMessageFacet twitterDirectMessageFacet = new TwitterDirectMessageFacet(apiData.updateInfo.apiKey.getId());
					super.extractCommonFacetData(twitterDirectMessageFacet, apiData);
					
					long createdAtTime = parseDate(twitterItem.getString("created_at"));
					twitterDirectMessageFacet.text = StringEscapeUtils.unescapeHtml(twitterItem.getString("text"));
					twitterDirectMessageFacet.start = createdAtTime;
					twitterDirectMessageFacet.end = createdAtTime;
					twitterDirectMessageFacet.time = createdAtTime;
					JSONObject sender = twitterItem.getJSONObject("sender");
					JSONObject recipient = twitterItem.getJSONObject("recipient");
					twitterDirectMessageFacet.senderProfileImageUrl = sender.getString("profile_image_url");
					twitterDirectMessageFacet.senderName = sender.getString("screen_name");
					twitterDirectMessageFacet.recipientName = recipient.getString("screen_name");
					twitterDirectMessageFacet.senderScreenName = twitterItem.getString("sender_screen_name");
					twitterDirectMessageFacet.recipientProfileImageUrl = recipient.getString("profile_image_url");
					twitterDirectMessageFacet.recipientScreenName = twitterItem.getString("recipient_screen_name");
					twitterDirectMessageFacet.twitterId = twitterItem.getLong("id");
                    if (updateInfo.getContext("sent")==null)
                        twitterDirectMessageFacet.sent = (byte) 0;
                    else
                        twitterDirectMessageFacet.sent = (byte) (updateInfo.getContext("sent").equals("1") ? 1 : 0);
					facets.add(twitterDirectMessageFacet);
					break;
                }
			}
		}
		
		return facets;
	}
	
	long parseDate(String dateString) {
		long time = format.parseMillis(dateString);
		return time;
	}
	
}
