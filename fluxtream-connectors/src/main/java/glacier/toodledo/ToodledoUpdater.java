package glacier.toodledo;

import java.security.NoSuchAlgorithmException;
import org.fluxtream.Configuration;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.Connector.UpdateStrategyType;
import org.fluxtream.connectors.RESTHelper;
import org.fluxtream.connectors.annotations.Updater;
import org.fluxtream.connectors.updaters.AbstractUpdater;
import org.fluxtream.connectors.updaters.RateLimitReachedException;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.services.GuestService;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.fluxtream.utils.Utils.hash;

/**
 * @author candide
 * 
 */

@Component
@Updater(prettyName = "Toodledo", value = 37, objectTypes = {
		ToodledoTaskFacet.class, ToodledoGoalFacet.class }, updateStrategyType = UpdateStrategyType.ALWAYS_UPDATE)
public class ToodledoUpdater extends AbstractUpdater {

	private static final String TOODLEDO_ACCOUNT_TOKEN = "http://api.toodledo.com/2/account/token.php";
	private static final String TOODLEDO_ACCOUNT_LOOKUP = "http://api.toodledo.com/2/account/lookup.php";
	private static final String TOODLEDO_ACCOUNT_INFO = "http://api.toodledo.com/2/account/get.php";
	private static final String TOODLEDO_TASKS_GET = "http://api.toodledo.com/2/tasks/get.php";
	private static final String TOODLEDO_TASKS_GET_DELETED = "http://api.toodledo.com/2/tasks/deleted.php";
	private static final String TOODLEDO_GOALS_GET = "http://api.toodledo.com/2/goals/get.php";

	@Autowired
	RESTHelper restHelper;

	public ToodledoUpdater() {
		super();
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		sync(updateInfo);
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws RateLimitReachedException, Exception {
		sync(updateInfo);
	}

	private void sync(UpdateInfo updateInfo) throws Exception {
		//String userKey = getKey(updateInfo);
		//String url = TOODLEDO_ACCOUNT_INFO + "?key=" + userKey;
		//String mostRecentJsonString = restHelper.makeRestCall(updateInfo.apiKey.getGuestId(), updateInfo.apiKey.getConnector(), -1, url);
		//JSONObject mostRecentAccountInfo = JSONObject
		//		.fromObject(mostRecentJsonString);
		//String lastAccountInfoJsonString = guestService.getApiKeyAttribute(
		//		updateInfo.apiKey, "lastAccountInfo");
		//JSONObject lastSyncAccountInfo = JSONObject
		//		.fromObject(lastAccountInfoJsonString);
        //
		//if (mostRecentAccountInfo.has("errorCode"))
		//	throw new Exception("Could not sync with toodledo: "
		//			+ mostRecentAccountInfo.getString("errorDesc"));
		//if (lastAccountInfoJsonString != null) {
		//	if (mostRecentAccountInfo.getLong("lastedit_goal") > lastSyncAccountInfo
		//			.getLong("lastedit_goal")) {
		//		retrieveGoals(updateInfo,
		//				lastSyncAccountInfo.getLong("lastedit_goal"), true);
		//	}
		//} else
		//	retrieveGoals(updateInfo, 0, false);
		//if (lastAccountInfoJsonString != null) {
		//	long lastTasksUpdateLocal = Math.max(
		//			lastSyncAccountInfo.getLong("lastedit_task"),
		//			lastSyncAccountInfo.getLong("lastdelete_task"));
		//	long lastTasksUpdateRemote = Math.max(
		//			mostRecentAccountInfo.getLong("lastedit_task"),
		//			mostRecentAccountInfo.getLong("lastdelete_task"));
		//	if (lastTasksUpdateRemote > lastTasksUpdateLocal) {
		//		retrieveTasks(updateInfo, lastTasksUpdateLocal, true);
		//	}
		//} else {
		//	retrieveTasks(updateInfo, 0, false);
		//}
		//guestService.setApiKeyAttribute(updateInfo.apiKey,
		//		"lastAccountInfo", mostRecentJsonString);
	}

	private void retrieveGoals(UpdateInfo updateInfo, long since, boolean update)
			throws RateLimitReachedException, Exception {
		//String key = getKey(updateInfo);
		//String urlString = TOODLEDO_GOALS_GET + "?key=" + key;
		//String goalsJson = restHelper.makeRestCall(updateInfo.apiKey.getGuestId(), updateInfo.apiKey.getConnector(), 2, urlString);
		//apiDataService.eraseApiData(updateInfo.apiKey, 2);
		//JSONArray goalsArray = JSONArray.fromObject(goalsJson);
		//for (int i = 0; i < goalsArray.size(); i++) {
		//	JSONObject goal = goalsArray.getJSONObject(i);
		//	ToodledoGoalFacet goalFacet = new ToodledoGoalFacet(updateInfo.apiKey.getId());
		//	goalFacet.archived = (byte) goal.getInt("archived");
		//	goalFacet.contributes = goal.getLong("contributes");
		//	goalFacet.level = goal.getInt("level");
		//	goalFacet.name = goal.getString("name");
		//	goalFacet.note = goal.getString("note");
		//	goalFacet.guestId = updateInfo.getGuestId();
		//	goalFacet.api = connector().value();
		//	goalFacet.objectType = 2;
		//	long now = System.currentTimeMillis();
		//	goalFacet.timeUpdated = now;
		//	jpaDaoService.persist(goalFacet);
		//}
	}

	private void retrieveTasks(UpdateInfo updateInfo, long since, boolean update)
			throws RateLimitReachedException, Exception {
		//String key = getKey(updateInfo);
		//String urlString = TOODLEDO_TASKS_GET + "?key=" + key + "&after="
		//		+ since;
		//String tasksJson = restHelper.makeRestCall(updateInfo.apiKey.getGuestId(), updateInfo.apiKey.getConnector(), 1, urlString);
		//JSONArray tasksArray = JSONArray.fromObject(tasksJson);
		//JSONObject arrayInfo = tasksArray.getJSONObject(0);
		//for (int i = 0; i < arrayInfo.getInt("total"); i++) {
		//	JSONObject task = tasksArray.getJSONObject(i + 1);
		//	if (task.has("total"))
		//		continue;
		//	long toodledo_id = task.getLong("id");
		//	if (update) {
		//		ToodledoTaskFacet oldTask = jpaDaoService.findOne(
		//				"toodledo.task.byToodledoId", ToodledoTaskFacet.class,
		//				updateInfo.getGuestId(), toodledo_id);
		//		if (oldTask != null)
		//			jpaDaoService.remove(oldTask.getClass(), oldTask.getId());
		//	}
		//	ToodledoTaskFacet taskFacet = new ToodledoTaskFacet(updateInfo.apiKey.getId());
		//	taskFacet.guestId = updateInfo.getGuestId();
		//	taskFacet.toodledo_id = toodledo_id;
		//	if (task.has("goal"))
		//		taskFacet.goal = task.getLong("goal");
		//	taskFacet.modified = task.getLong("modified");
		//	taskFacet.completed = task.getLong("completed");
		//	if (taskFacet.completed != 0) {
		//		taskFacet.start = taskFacet.modified * 1000;
		//		taskFacet.end = taskFacet.modified * 1000;
		//	}
		//	taskFacet.title = task.getString("title");
		//	taskFacet.api = connector().value();
		//	taskFacet.objectType = 1;
		//	jpaDaoService.persist(taskFacet);
		//}
		//if (update) {
		//	urlString = TOODLEDO_TASKS_GET_DELETED + "?key=" + key + "&after="
		//			+ since;
		//	String tasksToDeleteJson = restHelper.makeRestCall(updateInfo.apiKey.getGuestId(), updateInfo.apiKey.getConnector(), 1, urlString);
		//	JSONArray tasksToDeleteArray = JSONArray
		//			.fromObject(tasksToDeleteJson);
		//	arrayInfo = tasksToDeleteArray.getJSONObject(0);
		//	for (int i = 0; i < arrayInfo.getInt("num"); i++) {
		//		JSONObject task = tasksToDeleteArray.getJSONObject(i + 1);
		//		long toodledo_id = task.getLong("id");
		//		ToodledoTaskFacet taskToDelete = jpaDaoService.findOne(
		//				"toodledo.task.byToodledoId", ToodledoTaskFacet.class,
		//				updateInfo.getGuestId(), toodledo_id);
		//		if (taskToDelete != null)
		//			jpaDaoService.remove(taskToDelete.getClass(),
		//					taskToDelete.getId());
		//	}
		//}
	}

	private String getKey(UpdateInfo updateInfo) throws Exception {
		String sessionToken = getSessionToken(updateInfo);
		String userPassword = guestService.getApiKeyAttribute(updateInfo.apiKey, "password");
		return hash(hash(userPassword) + env.get("toodledo.appToken")
				+ sessionToken);
	}

	private String getSessionToken(UpdateInfo updateInfo) throws Exception {
		String token = "";
		String sessionTokenExpires = guestService.getApiKeyAttribute(updateInfo.apiKey, "sessionToken-expires");
		long now = System.currentTimeMillis();
		if (sessionTokenExpires == null
				|| Long.valueOf(sessionTokenExpires) < now) {
			long elapsed = now - Long.valueOf(sessionTokenExpires);
			System.out.println("token has expired since " + elapsed + " ms");
			String userid = guestService.getApiKeyAttribute(updateInfo.apiKey, "userid");
			token = getSessionToken(updateInfo, userid);
			guestService.setApiKeyAttribute(updateInfo.apiKey, "sessionToken", token);
			String fourHoursFromNow = String.valueOf(now + (3600000 * 4));
			guestService.setApiKeyAttribute(updateInfo.apiKey, "sessionToken-expires",
					fourHoursFromNow);
		} else
			token = guestService.getApiKeyAttribute(updateInfo.apiKey, "sessionToken");
		return token;
	}

	public boolean checkAuthorization(GuestService guestService, long guestId) {
		ApiKey apiKey = guestService.getApiKey(guestId,
				Connector.getConnector("toodledo"));
		return apiKey != null;
	}

	public String getSessionToken(UpdateInfo updateInfo, String userid)
			throws RateLimitReachedException, Exception {
		String appId = env.get("toodledo.appId");
		String appToken = env.get("toodledo.appToken");
		String signature = hash(userid + appToken);
		String url = TOODLEDO_ACCOUNT_TOKEN + "?userid=" + userid + ";appid="
				+ appId + ";sig=" + signature;
		try {
			String json = restHelper.makeRestCall(updateInfo.apiKey, -1, url);
			JSONObject accountTokenJson = JSONObject.fromObject(json);
			if (accountTokenJson.has("errorCode")) {
				String errorMessage = accountTokenJson.getString("errorDesc");
				throw new RuntimeException(
						"Could not get toodledo session token: " + errorMessage);
			}

			String token = accountTokenJson.getString("token");

			return token;
		} catch (Exception e) {
			throw e;
		}
	}

	public String getToodledoUserKey(Configuration env, String password,
			String sessionToken) throws NoSuchAlgorithmException {
		String hashed = hash(password);
		String appToken = env.get("toodledo.appToken");
		String toHash = hashed + appToken + sessionToken;
		return hash(toHash);
	}

	public String getToodledoUserid(long guestId, Connector connector, String email, String password)
			throws RateLimitReachedException, Exception {

		//String appId = env.get("toodledo.appId");
		//String appToken = env.get("toodledo.appToken");
		//String signature = hash(email + appToken);
        //
		//String url = TOODLEDO_ACCOUNT_LOOKUP + "?appid=" + appId + ";sig="
		//		+ signature + ";email=" + email + ";pass=" + password;
		//String json = restHelper.makeRestCall(guestId, connector, -1, url);
		//JSONObject accountLookupJson = JSONObject.fromObject(json);
		//String userid = accountLookupJson.getString("userid");
		//if (userid == null)
		//	return null;
		//return userid;
        return null;
	}

}
