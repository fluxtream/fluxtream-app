package com.fluxtream.connectors.twitter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;

@Component
public class TwitterFacetExtractor extends AbstractFacetExtractor {
	
	private static final DateTimeFormatter format = DateTimeFormat.forPattern("EEE MMM d HH:mm:ss Z yyyy");
	
	public List<AbstractFacet> extractFacets(ApiData apiData, ObjectType objectType) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

		JSONArray feed = JSONArray.fromObject(apiData.json);
				
		@SuppressWarnings("rawtypes")
		Iterator iterator = feed.iterator();
		while(iterator.hasNext()) {

			JSONObject twitterItem = (JSONObject)iterator.next();
			
			switch (objectType.value()) {
				case 1:
                {
					TweetFacet tweetFacet = new TweetFacet();
					super.extractCommonFacetData(tweetFacet, apiData);
		
					long createdAtTime = parseDate(twitterItem.getString("created_at"));
					tweetFacet.text = twitterItem.getString("text");
					tweetFacet.start = createdAtTime;
					tweetFacet.end = createdAtTime;
					tweetFacet.time = createdAtTime;
                    tweetFacet.tweetId = twitterItem.getLong("id");
                    JSONObject user = twitterItem.getJSONObject("user");
					tweetFacet.profileImageUrl = user.getString("profile_image_url");
					facets.add(tweetFacet);
					break;
                }
				case 4:
                {
					TwitterMentionFacet twitterMentionFacet = new TwitterMentionFacet();
					super.extractCommonFacetData(twitterMentionFacet, apiData);

                    long createdAtTime = parseDate(twitterItem.getString("created_at"));
					twitterMentionFacet.text = twitterItem.getString("text");
					twitterMentionFacet.start = createdAtTime;
					twitterMentionFacet.end = createdAtTime;
					twitterMentionFacet.time = createdAtTime;
					twitterMentionFacet.twitterId = twitterItem.getLong("id");
					JSONObject user = twitterItem.getJSONObject("user");
					twitterMentionFacet.profileImageUrl = user.getString("profile_image_url");
					twitterMentionFacet.userName = user.getString("name");
					facets.add(twitterMentionFacet);
					break;
                }
				case 2:
                {
					TwitterDirectMessageFacet twitterDirectMessageFacet = new TwitterDirectMessageFacet();
					super.extractCommonFacetData(twitterDirectMessageFacet, apiData);
					
					long createdAtTime = parseDate(twitterItem.getString("created_at"));
					twitterDirectMessageFacet.text = twitterItem.getString("text");
					twitterDirectMessageFacet.start = createdAtTime;
					twitterDirectMessageFacet.end = createdAtTime;
					twitterDirectMessageFacet.time = createdAtTime;
					JSONObject sender = twitterItem.getJSONObject("sender");
					JSONObject recipient = twitterItem.getJSONObject("recipient");
					twitterDirectMessageFacet.senderProfileImageUrl = sender.getString("profile_image_url");
					twitterDirectMessageFacet.senderName = sender.getString("name");
					twitterDirectMessageFacet.recipientName = recipient.getString("name");
					twitterDirectMessageFacet.senderScreenName = twitterItem.getString("sender_screen_name");
					twitterDirectMessageFacet.recipientProfileImageUrl = recipient.getString("profile_image_url");
					twitterDirectMessageFacet.recipientScreenName = twitterItem.getString("recipient_screen_name");
					twitterDirectMessageFacet.twitterId = twitterItem.getLong("id");
					String screen_name = (String) this.updateInfo.getContext("screen_name");
					twitterDirectMessageFacet.sent = (byte)(twitterDirectMessageFacet.senderScreenName.equals(screen_name)?1:0);
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
