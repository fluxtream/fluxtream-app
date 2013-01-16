package com.fluxtream.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import com.fluxtream.Configuration;
import com.fluxtream.auth.FlxUserDetails;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.OAuth2Helper;
import com.fluxtream.connectors.google_latitude.LocationFacet;
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
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class GuestServiceImpl implements GuestService {

	@Autowired
	Configuration env;

	@PersistenceContext
	EntityManager em;

    @Qualifier("apiDataServiceImpl")
    @Autowired
	ApiDataService apiDataService;

    @Qualifier("metadataServiceImpl")
    @Autowired
	MetadataService metadataService;

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
	ConnectorUpdateService connectorUpdateService;

    @Autowired
    OAuth2Helper oAuth2Helper;

	LookupService geoIpLookupService;

	private final RandomString randomString = new RandomString(64);

	private UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		final Guest guest = JPAUtils.findUnique(em, Guest.class,
				"guest.byUsername", username);
		if (guest == null)
			return null;
        return new FlxUserDetails(guest);
	}

	public UserDetails loadUserByEmail(String email)
			throws UsernameNotFoundException {
		final Guest guest = JPAUtils.findUnique(em, Guest.class,
				"guest.byEmail", email);
		if (guest == null)
			return null;
        return new FlxUserDetails(guest);
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
        return JPAUtils.findUnique(em, Guest.class,
                "guest.byUsername", username);
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
		}
        removeApiKeyAttribute(guestId, apiKey, key);
		ApiKeyAttribute attr = new ApiKeyAttribute();
		attr.attributeKey = key;
		attr.setAttributeValue(value, env);
		em.persist(attr);
		apiKey.setAttribute(attr);
		em.merge(apiKey);
		return apiKey;
	}

    @Transactional(readOnly = false)
    private void removeApiKeyAttribute(long guestId, ApiKey apiKey, String key) {
        List<ApiKeyAttribute> atts = JPAUtils.find(em, ApiKeyAttribute.class, "apiKeyAttribute.byKeyAndConnector", apiKey, key);
        for (ApiKeyAttribute att : atts)
            em.remove(att);
    }

	@Transactional(readOnly = false)
	@Override
	public void removeApiKey(long guestId, Connector connector) {
		ApiKey apiKey = JPAUtils.findUnique(em, ApiKey.class, "apiKey.byApi",
				guestId, connector.value());
        final String refreshTokenRemoveURL = apiKey.getAttributeValue("refreshTokenRemoveURL", env);
        // Revoke refresh token might throw.  If it does we still want to remove the apiKeys from
        // the DB which is why we put it in a try/finally block
        try {
             if (refreshTokenRemoveURL !=null)
                oAuth2Helper.revokeRefreshToken(guestId, connector, refreshTokenRemoveURL);
        }
        finally {
            em.remove(apiKey);
            if (connector == Connector.getConnector("google_latitude"))
                JPAUtils.execute(em, "context.delete.all", guestId);
            JPAUtils.execute(em, "apiUpdates.delete.byApi", guestId, connector.value());
            connectorUpdateService.flushUpdateWorkerTasks(guestId, connector, true);
            apiDataService.eraseApiData(guestId, connector);
        }
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
    public Map<String, String> getApiKeyAttributes(final long guestId, final Connector api, final String key) {
        ApiKey apiKey = JPAUtils.findUnique(em, ApiKey.class, "apiKey.byApi",
                                            guestId, api.value());
        if (apiKey == null)
            return null;

        return apiKey.getAttributes(env);
    }

    @Override
	public List<ApiKey> getApiKeys(long guestId) {
        return JPAUtils.find(em, ApiKey.class, "apiKeys.all",
                guestId);
	}

	@Override
	public boolean hasApiKey(long guestId, Connector api) {
		ApiKey apiKey = JPAUtils.findUnique(em, ApiKey.class, "apiKey.byApi",
				guestId, api.value());
		return (apiKey != null);
	}

	@Override
	public ApiKey getApiKey(long guestId, Connector api) {
        return JPAUtils.findUnique(em, ApiKey.class, "apiKey.byApi",
                guestId, api.value());
	}

	@Override
	@Transactional(readOnly = false)
    @Secured("ROLE_ADMIN")
	public void eraseGuestInfo(String username) throws Exception {
		Guest guest = getGuest(username);
		if (guest == null)
			return;
		List<ApiKey> apiKeys = getApiKeys(guest.getId());
		for (ApiKey key : apiKeys) {
            if(key!=null && key.getConnector()!=null) {
			    apiDataService.eraseApiData(guest.getId(), key.getConnector());
            }
		}
		for (ApiKey apiKey : apiKeys) {
            if(apiKey!=null){
			    em.remove(apiKey);
            }
		}
		JPAUtils.execute(em, "addresses.delete.all", guest.getId());
		JPAUtils.execute(em, "notifications.delete.all", guest.getId());
		JPAUtils.execute(em, "settings.delete.all", guest.getId());
		JPAUtils.execute(em, "context.delete.all", guest.getId());
		JPAUtils.execute(em, "updateWorkerTasks.delete.all", guest.getId());
        JPAUtils.execute(em, "tags.delete.all", guest.getId());
        JPAUtils.execute(em, "notifications.delete.all", guest.getId());
        JPAUtils.execute(em, "coachingBuddies.delete.all", guest.getId());
		em.remove(guest);
	}

	@Override
    @Secured("ROLE_ADMIN")
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
	@Secured("ROLE_ADMIN")
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
	@Secured("ROLE_ADMIN")
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
        return JPAUtils.findUnique(em,
                ResetPasswordToken.class, "passwordToken.byToken", token);
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
        return JPAUtils.findUnique(em, Guest.class,
                "guest.byEmail", email);
	}

	@Override
	public void deleteToken(String token) {
		ResetPasswordToken ptoken = JPAUtils.findUnique(em,
				ResetPasswordToken.class, "passwordToken.byToken", token);
		em.remove(ptoken);
	}

	@Override
	public void checkIn(long guestId, String ipAddress) throws IOException {
		if (SecurityUtils.isStealth())
			return;
		if (geoIpLookupService == null) {
			String dbLocation = env.get("geoIpDb.location");
			geoIpLookupService = new LookupService(dbLocation,
					LookupService.GEOIP_MEMORY_CACHE);
		}
		Location ipLocation = geoIpLookupService.getLocation(ipAddress);
        long time = System.currentTimeMillis();
        LocationFacet locationFacet = new LocationFacet();
        locationFacet.timestampMs = time;
        locationFacet.start = time;
        locationFacet.end = time;
        locationFacet.guestId = guestId;
		if (ipLocation != null) {
            locationFacet.accuracy = 7000;
            locationFacet.latitude = ipLocation.latitude;
            locationFacet.longitude = ipLocation.longitude;
            locationFacet.source = LocationFacet.Source.GEO_IP_DB;
            apiDataService.addGuestLocation(guestId,
					locationFacet);
		} else if (env.get("environment").equals("local")) {
            try{
                locationFacet.accuracy = 7000;
                locationFacet.latitude = env.getFloat("defaultLocation.latitude");
                locationFacet.longitude = env.getFloat("defaultLocation.longitude");
                locationFacet.source = LocationFacet.Source.OTHER;
                apiDataService.addGuestLocation(guestId,
                        locationFacet);
            }
            catch (Exception ignored){
            }
		} else {
            String ip2locationKey = env.get("ip2location.apiKey");
			String jsonString = HttpUtils.fetch(
					"http://api.ipinfodb.com/v3/ip-city/?key=" + ip2locationKey
							+ "&ip=" + ipAddress + "&format=json");
			JSONObject json = JSONObject.fromObject(jsonString);
			String latitude = json.getString("latitude");
			String longitude = json.getString("longitude");
            locationFacet.latitude = Float.valueOf(latitude);
            locationFacet.longitude = Float.valueOf(longitude);
            if (latitude != null && longitude != null) {
				float lat = Float.valueOf(latitude);
				float lon = Float.valueOf(longitude);
                locationFacet.accuracy = 7000;
                locationFacet.latitude = lat;
                locationFacet.longitude = lon;
                locationFacet.source = LocationFacet.Source.IP_TO_LOCATION;
                apiDataService.addGuestLocation(guestId,
						locationFacet);
			}
		}
	}

}
