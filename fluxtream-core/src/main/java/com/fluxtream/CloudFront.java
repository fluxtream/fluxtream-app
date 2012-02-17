package com.fluxtream;

import org.codehaus.plexus.util.StringUtils;

public class CloudFront {

	public static String cache(String url) {
		String cached = StringUtils.replace(url, "http://images.amazon.com", "https://djpp1awnsjdly.cloudfront.net");
		cached = StringUtils.replace(cached, "http://userserve-ak.last.fm", "https://d3nmbueh24y8gu.cloudfront.net");
		return cached;
	}
	
}
