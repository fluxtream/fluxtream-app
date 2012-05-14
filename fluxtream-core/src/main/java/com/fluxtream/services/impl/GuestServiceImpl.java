package com.fluxtream.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.fluxtream.connectors.google_latitude.LocationFacet;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fluxtream.Configuration;
import com.fluxtream.auth.FlxUserDetails;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.AbstractUserProfile;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiKeyAttribute;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.ResetPasswordToken;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.JPAUtils;
import com.fluxtream.utils.RandomString;
import com.fluxtream.utils.SecurityUtils;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

@Transactional(readOnly = true)
@Service
public class GuestServiceImpl implements GuestService {

	@Autowired
	Configuration env;

	@PersistenceContext
	EntityManager em;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	MetadataService metadataService;

	@Autowired
	ConnectorUpdateService connectorUpdateService;

	LookupService geoIpLookupService;

	private final RandomString randomString = new RandomString(64);

	private UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		final Guest guest = JPAUtils.findUnique(em, Guest.class,
				"guest.byUsername", username);
		if (guest == null)
			return null;
		FlxUserDetails user = new FlxUserDetails(guest);
		return user;
	}

	public UserDetails loadUserByEmail(String email)
			throws UsernameNotFoundException {
		final Guest guest = JPAUtils.findUnique(em, Guest.class,
				"guest.byEmail", email);
		if (guest == null)
			return null;
		FlxUserDetails user = new FlxUserDetails(guest);
		return user;
	}

	@Transactional(readOnly = false)
	public Guest createGuest(String username, String firstname,
			String lastname, String password, String email) throws Exception {
		if (loadUserByUsername(username) != null)
			throw new Exception("Username already taken");
		if (loadUserByEmail(email) != null)
			throw new Exception("Email already taken");
		Guest guest = new Guest();
		guest.username = username;
		guest.email = email;
		guest.firstname = firstname;
		guest.lastname = lastname;
		setPassword(guest, password);
		em.persist(guest);
//		createBodyTrackUser()....
		return guest;
	}

	private void setPassword(Guest guest, String password) {
		ShaPasswordEncoder passwordEncoder = new ShaPasswordEncoder();
		String salt = randomString.nextString();
		guest.salt = salt;
		guest.password = passwordEncoder.encodePassword(password, salt);
	}

	@Override
	@Transactional(readOnly = false)
	public void setPassword(long guestId, String password) {
		Guest guest = getGuestById(guestId);
		setPassword(guest, password);
		em.persist(guest);
	}

	@Override
	public Guest getGuest(String username) {
		final Guest guest = JPAUtils.findUnique(em, Guest.class,
				"guest.byUsername", username);
		return guest;
	}

	@Override
	public boolean isUsernameAvailable(String username) {
		final Guest guest = JPAUtils.findUnique(em, Guest.class,
				"guest.byUsername", username);
		return guest == null;
	}

	@Override
	public Guest getGuestById(long id) {
		return em.find(Guest.class, id);
	}

	@Override
	@Transactional(readOnly = false)
	public ApiKey setApiKeyAttribute(long guestId, Connector api, String key,
			String value) {
		ApiKey apiKey = JPAUtils.findUnique(em, ApiKey.class, "apiKey.byApi",
				guestId, api.value());
		if (apiKey == null) {
			apiKey = new ApiKey();
			apiKey.setGuestId(guestId);
			apiKey.setConnector(api);
			em.persist(apiKey);
			updateConnectorConfigStateKey(guestId);
		}
		ApiKeyAttribute attr = new ApiKeyAttribute();
		attr.attributeKey = key;
		attr.setAttributeValue(value, env);
		em.persist(attr);
		apiKey.setAttribute(attr);
		em.merge(apiKey);
		return apiKey;
	}

	@Transactional(readOnly = false)
	@Override
	public void removeApiKey(long guestId, Connector connector) {
		ApiKey apiKey = JPAUtils.findUnique(em, ApiKey.class, "apiKey.byApi",
				guestId, connector.value());
		if (connector != null)
			em.remove(apiKey);
		if (connector == Connector.getConnector("google_latitude"))
			JPAUtils.execute(em, "context.delete.all", guestId);
		apiDataService.eraseApiData(guestId, connector);
		connectorUpdateService.deleteScheduledUpdates(guestId, connector);
		updateConnectorConfigStateKey(guestId);
	}

	@Override
	public String getApiKeyAttribute(long guestId, Connector api, String key) {
		ApiKey apiKey = JPAUtils.findUnique(em, ApiKey.class, "apiKey.byApi",
				guestId, api.value());
		if (apiKey == null)
			return null;
		return apiKey.getAttributeValue(key, env);
	}

	@Override
	public List<ApiKey> getApiKeys(long guestId) {
		List<ApiKey> keys = JPAUtils.find(em, ApiKey.class, "apiKeys.all",
				guestId);
		return keys;
	}

	@Override
	public boolean hasApiKey(long guestId, Connector api) {
		ApiKey apiKey = JPAUtils.findUnique(em, ApiKey.class, "apiKey.byApi",
				guestId, api.value());
		return (apiKey != null);
	}

	@Override
	public ApiKey getApiKey(long guestId, Connector api) {
		ApiKey apiKey = JPAUtils.findUnique(em, ApiKey.class, "apiKey.byApi",
				guestId, api.value());
		return apiKey;
	}

	@Override
	@Transactional(readOnly = false)
	public void eraseGuestInfo(String username) throws Exception {
		Guest guest = getGuest(username);
		if (guest == null)
			return;
		List<ApiKey> apiKeys = getApiKeys(guest.getId());
		for (ApiKey key : apiKeys) {
			apiDataService.eraseApiData(guest.getId(), key.getConnector());
		}
		for (ApiKey apiKey : apiKeys) {
			em.remove(apiKey);
		}
		JPAUtils.execute(em, "addresses.delete.all", guest.getId());
		JPAUtils.execute(em, "notifications.delete.all", guest.getId());
		JPAUtils.execute(em, "settings.delete.all", guest.getId());
		JPAUtils.execute(em, "context.delete.all", guest.getId());
		JPAUtils.execute(em, "scheduledUpdates.delete.all", guest.getId());
		em.remove(guest);
	}

	@Override
	@Secured({ "ROLE_ADMIN", "ROLE_ROOT" })
	public List<Guest> getAllGuests() {
		List<Guest> all = JPAUtils.find(em, Guest.class, "guests.all",
				(Object[]) null);
		List<Guest> result = new ArrayList<Guest>();
		for (Guest guest : all)
			result.add(guest);
		return result;
	}

	@Override
	@Transactional(readOnly = false)
	@Secured({ "ROLE_ADMIN", "ROLE_ROOT" })
	public void addRole(long guestId, String role) {
		Guest guest = getGuestById(guestId);
		if (guest.hasRole(role))
			return;
		List<String> userRoles = guest.getUserRoles();
		userRoles.add(role);
		persistUserRoles(guest, userRoles);
	}

	@Override
	@Transactional(readOnly = false)
	@Secured({ "ROLE_ADMIN", "ROLE_ROOT" })
	public void removeRole(long guestId, String role) {
		Guest guest = getGuestById(guestId);
		if (!guest.hasRole(role))
			return;
		List<String> userRoles = guest.getUserRoles();
		userRoles.remove(role);
		persistUserRoles(guest, userRoles);
	}

	private void persistUserRoles(Guest guest, List<String> userRoles) {
		StringBuffer roles = new StringBuffer();
		for (int i = 0; i < userRoles.size(); i++) {
			if (i > 0)
				roles.append(",");
			roles.append(userRoles.get(i));
		}
		guest.roles = roles.toString();
		em.persist(guest);
	}

	@Override
	@Transactional(readOnly = false)
	public void saveUserProfile(long guestId, AbstractUserProfile userProfile) {
		userProfile.guestId = guestId;
		em.persist(userProfile);
	}

	@Override
	public <T extends AbstractUserProfile> T getUserProfile(long guestId,
			Class<T> clazz) {
		CriteriaBuilder qb = em.getCriteriaBuilder();
		CriteriaQuery<T> c = qb.createQuery(clazz);
		Root<T> from = c.from(clazz);
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		Predicate guestPredicate = criteriaBuilder.equal(from.get("guestId"),
				guestId);
		c.where(guestPredicate);
		TypedQuery<T> query = em.createQuery(c);
		List<T> resultList = query.getResultList();
		if (resultList.size() > 0)
			return resultList.get(0);
		return null;
	}

	@Override
	public ResetPasswordToken getToken(String token) {
		ResetPasswordToken ptoken = JPAUtils.findUnique(em,
				ResetPasswordToken.class, "passwordToken.byToken", token);
		return ptoken;
	}

	@Override
	@Transactional(readOnly = false)
	public ResetPasswordToken createToken(long guestId) {
		ResetPasswordToken pToken = new ResetPasswordToken();
		pToken.guestId = guestId;
		pToken.token = randomString.nextString();
		pToken.ts = System.currentTimeMillis();
		em.persist(pToken);
		return pToken;
	}

	@Override
	public Guest getGuestByEmail(String email) {
		final Guest guest = JPAUtils.findUnique(em, Guest.class,
				"guest.byEmail", email);
		return guest;
	}

	@Override
	public void deleteToken(String token) {
		ResetPasswordToken ptoken = JPAUtils.findUnique(em,
				ResetPasswordToken.class, "passwordToken.byToken", token);
		em.remove(ptoken);
	}

	@Override
	@Transactional(readOnly = false)
	public void checkIn(long guestId, String ipAddress) throws IOException {
		if (SecurityUtils.isStealth())
			return;
		if (geoIpLookupService == null) {
			String dbLocation = env.get("geoIpDb.location");
			geoIpLookupService = new LookupService(dbLocation,
					LookupService.GEOIP_MEMORY_CACHE);
		}
		Location ipLocation = geoIpLookupService.getLocation(ipAddress);
		if (ipLocation != null) {
			apiDataService.addGuestLocation(guestId,
					System.currentTimeMillis(), ipLocation.latitude,
					ipLocation.longitude, LocationFacet.Source.GEO_IP_DB);
		} else if (env.get("environment").equals("local")) {
            apiDataService.addGuestLocation(guestId,
					System.currentTimeMillis(), env.getFloat("defaultLocation.latitude"),
                    env.getFloat("defaultLocation.longitude"),
                    LocationFacet.Source.OTHER);
		} else {
			String ip2locationKey = env.get("ip2location.apiKey");
			String jsonString = HttpUtils.fetch(
					"http://api.ipinfodb.com/v3/ip-city/?key=" + ip2locationKey
							+ "&ip=" + ipAddress + "&format=json", env);
			JSONObject json = JSONObject.fromObject(jsonString);
			String latitude = json.getString("latitude");
			String longitude = json.getString("longitude");
			if (latitude != null && longitude != null) {
				float lat = Float.valueOf(latitude);
				float lon = Float.valueOf(longitude);
                apiDataService.addGuestLocation(guestId,
						System.currentTimeMillis(), lat, lon,
                        LocationFacet.Source.IP_TO_LOCATION);
			}
		}
	}

	@Override
	@Transactional(readOnly = false)
	public void updateConnectorConfigStateKey(long guestId) {
		Guest guest = getGuestById(guestId);
		guest.connectorConfigStateKey = randomString.nextString();
	}

}
