package org.fluxtream.connectors.lastfm;

import net.sf.json.JSONObject;

class LastfmHelper {

	public static String getSessionKey(String jsonResponse) {
		JSONObject json = JSONObject.fromObject(jsonResponse);
		if (!json.has("session")) return null;
		JSONObject session = json.getJSONObject("session");
		if (!session.has("key")) return null;
		return session.getString("key");
	}

	public static String getUsername(String jsonResponse) {
		JSONObject json = JSONObject.fromObject(jsonResponse);
		if (!json.has("session")) return null;
		JSONObject session = json.getJSONObject("session");
		if (!session.has("name")) return null;
		return session.getString("name");
	}
	
}
