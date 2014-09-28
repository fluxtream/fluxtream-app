package org.fluxtream.core.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import net.sf.json.JSONObject;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.DefaultHttpClient;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.FlxUserDetails;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.OAuth2Helper;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.domain.AbstractUserProfile;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ApiKeyAttribute;
import org.fluxtream.core.domain.CoachingBuddy;
import org.fluxtream.core.domain.ConnectorInfo;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.ResetPasswordToken;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.SettingsService;
import org.fluxtream.core.services.SystemService;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.JPAUtils;
import org.fluxtream.core.utils.RandomString;
import org.fluxtream.core.utils.SecurityUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class GuestServiceImpl implements GuestService, DisposableBean {

    static FlxLogger logger = FlxLogger.getLogger(GuestServiceImpl.class);

    @Autowired
    BodyTrackHelper bodyTrackHelper;

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

    @Autowired
    @Qualifier("AsyncWorker")
    ThreadPoolTaskExecutor executor;

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    SystemService systemService;

    @Autowired
    SettingsService settingsService;

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
	public Guest createGuest(String username, String firstname, String lastname,
                             String password, String email, Guest.RegistrationMethod registrationMethod,
                             final String appId)
            throws UsernameAlreadyTakenException, ExistingEmailException
    {
		if (loadUserByUsername(username) != null)
			throw new UsernameAlreadyTakenException(username + " is already taken");
		if (loadUserByEmail(email) != null)
			throw new ExistingEmailException(email + " is already used");
		Guest guest = new Guest();
		guest.username = username;
		guest.email = email;
		guest.firstname = firstname;
		guest.lastname = lastname;
        guest.registrationMethod = registrationMethod;
        guest.setRoles("ROLE_USER");
        if (password!=null)
    		setPassword(guest, password);
        if (appId!=null) {
            // enforce API registration method
            guest.registrationMethod = Guest.RegistrationMethod.REGISTRATION_METHOD_API;
            guest.appId = appId;
        }
		em.persist(guest);

		return guest;
	}

	private void setPassword(Guest guest, String password) {
		ShaPasswordEncoder passwordEncoder = new ShaPasswordEncoder();
		String salt = randomString.nextString();
		guest.salt = salt;
        if (guest.registrationMethod == Guest.RegistrationMethod.REGISTRATION_METHOD_FACEBOOK)
            guest.registrationMethod = Guest.RegistrationMethod.REGISTRATION_METHOD_FACEBOOK_WITH_PASSWORD;
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
    @Transactional(readOnly=false)
    public ApiKey createApiKey(final long guestId, final Connector connector) {
        ApiKey apiKey = new ApiKey();
        apiKey.setGuestId(guestId);
        apiKey.setConnector(connector);
        em.persist(apiKey);
        populateApiKey(apiKey.getId());
        return apiKey;
    }

    @Override
    @Transactional(readOnly=false)
    public void populateApiKey(final long apiKeyId) {
        ConnectorInfo connectorInfo = null;
        ApiKey apiKey = getApiKey(apiKeyId);
        try { connectorInfo = systemService.getConnectorInfo(apiKey.getConnector().getName());
        } catch (Throwable e) {}
        if (connectorInfo == null || !connectorInfo.enabled)
            throw new RuntimeException("This connector is not enabled!");
        final String[] apiKeyAttributesKeys = connectorInfo.getApiKeyAttributesKeys();
        if(apiKeyAttributesKeys!=null) {
            for (String key : apiKeyAttributesKeys) {
                if (env.get(key)==null)
                    throw new RuntimeException("No value was found for key :" + key + ". Cannot create apiKey");
                setApiKeyAttribute(apiKey, key, env.get(key));
            }
        }
    }

    @Override
    @Transactional(readOnly=false)
    public void addDeveloperRole(Long guestId) {
        Guest guest = getGuestById(guestId);
        if (guest.hasRole("ROLE_DEVELOPER"))
            return;
        List<String> userRoles = guest.getUserRoles();
        userRoles.add("ROLE_DEVELOPER");
        persistUserRoles(guest, userRoles);
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
	public ApiKey setApiKeyAttribute(ApiKey ak, String key,
			String value) {
        ApiKey apiKey = em.find(ApiKey.class, ak.getId(), LockModeType.PESSIMISTIC_WRITE);

        // apiKey could be null, for example if the connector
        // was already deleted.  In this case just return
        // null
        if(apiKey==null) {
            return null;
        }
        // At this point we know that apiKey exists and
        // is non-null
        apiKey.removeAttribute(key);
        ApiKeyAttribute attr = new ApiKeyAttribute();
		attr.attributeKey = key;
		attr.setAttributeValue(value, env);
		em.persist(attr);
		apiKey.setAttribute(attr);
        em.merge(apiKey);
		return apiKey;
	}

    @Override
    public Map<String, String> getApiKeyAttributes(final long apiKeyId) {
        ApiKey apiKey = em.find(ApiKey.class, apiKeyId);
        final Map<String, String> attributes = apiKey.getAttributes(env);
        return attributes;
    }

    @Transactional(readOnly = false)
	@Override
	public void removeApiKey(long apiKeyId) {
		ApiKey apiKey = em.find(ApiKey.class, apiKeyId);

        // apiKey could be null, for example if the connector
        // was already deleted.  In this case just return
        if(apiKey==null) {
            return;
        }

        final String refreshTokenRemoveURL = apiKey.getAttributeValue("refreshTokenRemoveURL", env);
        // Revoke refresh token might throw.  If it does we still want to remove the apiKeys from
        // the DB which is why we put it in a try/finally block
        try {
             if (refreshTokenRemoveURL !=null)
                oAuth2Helper.revokeRefreshToken(apiKey.getGuestId(), apiKey.getConnector(), refreshTokenRemoveURL);
        }
        finally {
            em.remove(apiKey);
            em.flush();
            // cleanup the data asynchrously in order not to block the user's flow
            bodyTrackHelper.deleteChannelMappings(apiKey);
            ApiDataCleanupWorker worker = beanFactory.getBean(ApiDataCleanupWorker.class);
            worker.setApiKey(apiKey);
            executor.execute(worker);
        }
	}

    @Override
    @Deprecated
	public String getApiKeyAttribute(ApiKey ak, String key) {
        ApiKey apiKey = em.find(ApiKey.class, ak.getId());
        // apiKey could be null, for example if the connector
        // was deleted.  In this case return null
        if(apiKey!=null) {
		    return apiKey.getAttributeValue(key, env);
        }
        else {
            return null;
        }
	}

    @Override
    public ApiKey getApiKey(final long apiKeyId) {
        return em.find(ApiKey.class, apiKeyId);
    }

    @Override
	public List<ApiKey> getApiKeys(long guestId) {
        // rawKeys includes all the keys in the apiKeys table for a given guest.
        // However, it's potentially the case that this could include keys which
        // do not currently map to valid entries in the Connector table.
        // We can test for that condition by checking if key.getConnector() returns
        // null.  Include only the keys which return non-null in goodKeys and return.
        List<ApiKey> rawKeys = JPAUtils.find(em, ApiKey.class, "apiKeys.all",
                    guestId);
        List<ApiKey> goodKeys = new ArrayList<ApiKey>();
        for (ApiKey key : rawKeys){
            if(key.getConnector()!=null) {
                goodKeys.add(key);
            }
        }
        return(goodKeys);
	}

	@Override
	public boolean hasApiKey(long guestId, Connector api) {
        assert api!=null : "api must not be null";
		ApiKey apiKey = JPAUtils.findUnique(em, ApiKey.class, "apiKey.byApi",
				guestId, api.value());
		return (apiKey != null);
	}

	@Override
	public List<ApiKey> getApiKeys(long guestId, Connector api) {
        List<ApiKey> apiKeys = JPAUtils.find(em, ApiKey.class, "apiKey.byApi", guestId, api.value());
        Collections.sort(apiKeys, new Comparator<ApiKey>() {
            @Override
            public int compare(ApiKey o1, ApiKey o2) {
                return (int)(o2.getId()-o1.getId());
            }
        });
        return apiKeys;
	}

    @Override
    @Transactional(readOnly=false)
    public void setApiKeyStatus(final long apiKeyId, final ApiKey.Status status, final String stackTrace,
                                final String reason) {
        final ApiKey apiKey = getApiKey(apiKeyId);
        if (apiKey!=null) {
            apiKey.status = status;
            if (status== ApiKey.Status.STATUS_UP) {
                apiKey.stackTrace = null;
                apiKey.reason = null;
            } else {
                if (stackTrace != null)
                    apiKey.stackTrace = stackTrace;
                if (reason != null)
                    apiKey.reason = reason;
            }
            em.persist(apiKey);
        }
    }

    @Override
    @Transactional(readOnly=false)
    public void setApiKeyToSynching(final long apiKeyId, final boolean synching) {
        final ApiKey apiKey = getApiKey(apiKeyId);
        if (apiKey!=null) {
            apiKey.synching = synching;
            em.persist(apiKey);
        }
    }

    @Override
    @Deprecated
    public ApiKey getApiKey(long guestId, Connector api) {
        List<ApiKey> apiKeys = getApiKeys(guestId, api);
        return apiKeys.size()>0
                ? apiKeys.get(0)
                : null;
    }

    @Override
    @Transactional(readOnly=false)
    public void removeApiKeys(final long guestId, final Connector connector) {
        final List<ApiKey> apiKeys = getApiKeys(guestId, connector);
        for (ApiKey apiKey : apiKeys)
            removeApiKey(apiKey.getId());
    }

    @Override
    @Transactional(readOnly = false)
    public void eraseGuestInfo(long id) throws Exception {
        Guest guest = getGuestById(id);
        if (guest == null)
            return;
        if (guest.registrationMethod==Guest.RegistrationMethod.REGISTRATION_METHOD_FACEBOOK||
            guest.registrationMethod==Guest.RegistrationMethod.REGISTRATION_METHOD_FACEBOOK_WITH_PASSWORD) {
            revokeFacebookPermissions(guest);
        }
        JPAUtils.execute(em, "updateWorkerTasks.delete.all", guest.getId());
        em.remove(guest);
        em.flush();
        List<ApiKey> apiKeys = getApiKeys(guest.getId());
        for (ApiKey key : apiKeys) {
            if(key!=null && key.getConnector()!=null) {
                apiDataService.eraseApiData(key);
                em.flush();
            }
        }
        em.flush();
        for (ApiKey apiKey : apiKeys) {
            if(apiKey!=null){
                em.remove(apiKey);
                em.flush();
            }
        }
        em.flush();
        JPAUtils.execute(em, "addresses.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "notifications.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "settings.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "location.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "visitedCities.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "updateWorkerTasks.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "tags.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "notifications.delete.all", guest.getId());
        em.flush();
        final List<CoachingBuddy> coachingBuddies = JPAUtils.find(em, CoachingBuddy.class, "coachingBuddies.byGuestId", guest.getId());
        for (CoachingBuddy coachingBuddy : coachingBuddies)
            em.remove(coachingBuddy);
        em.flush();
        JPAUtils.execute(em, "channelMapping.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "connectorFilterState.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "channelStyle.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "grapherView.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "widgetSettings.delete.all", guest.getId());
        em.flush();
        JPAUtils.execute(em, "dashboards.delete.all", guest.getId());
        em.flush();
    }

    private void revokeFacebookPermissions(final Guest guest) {
        ApiKey facebookApiKey = getApiKey(guest.getId(), Connector.getConnector("facebook"));
        final String meString = getApiKeyAttribute(facebookApiKey, "me");
        final String accessToken = getApiKeyAttribute(facebookApiKey, "accessToken");
        JSONObject meJSON = JSONObject.fromObject(meString);
        String id = meJSON.getString("id");
        HttpClient client = new DefaultHttpClient();
        try {
            final String uri = String.format("https://graph.facebook.com/%s/permissions?access_token=%s", id, accessToken);
            HttpDelete delete = new HttpDelete(uri);
            client.execute(delete);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Override
	@Transactional(readOnly = false)
    @Secured("ROLE_ADMIN")
	public void eraseGuestInfo(String username) throws Exception {
        Guest guest = getGuest(username);
        eraseGuestInfo(guest.getId());
	}

	@Override
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
		guest.setRoles(roles.toString());
		em.persist(guest);
	}

	@Override
	@Transactional(readOnly = false)
	public void saveUserProfile(long guestId, AbstractUserProfile userProfile) {
		userProfile.guestId = guestId;
		em.persist(userProfile);
	}

    @Override
    @Transactional(readOnly=false)
    public void setApiKeySettings(final long apiKeyId, final Object settings) {
        ApiKey apiKey = getApiKey(apiKeyId);
        apiKey.setSettings(settings);
        em.persist(apiKey);
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
    @Transactional(readOnly=false)
    public void removeApiKeyAttribute(final long apiKeyId, final String key) {
        ApiKey apiKey = getApiKey(apiKeyId);
        apiKey.removeAttribute(key);
        em.persist(apiKey);
    }

    @Override
    @Transactional(readOnly=false)
    public void setAutoLoginToken(final long guestId, final String s) {
        Guest guest = getGuestById(guestId);
        guest.autoLoginToken = s;
        guest.autoLoginTokenTimestamp = System.currentTimeMillis();
        em.persist(guest);
    }

    @Override
    public boolean checkPassword(final long guestId, final String currentPassword) {
        Guest guest = getGuestById(guestId);
        ShaPasswordEncoder passwordEncoder = new ShaPasswordEncoder();
        String password = passwordEncoder.encodePassword(currentPassword, guest.salt);
        return password.equals(guest.password);
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

        LocationFacet locationFacet = new LocationFacet(1);
        long time = System.currentTimeMillis();
        locationFacet.guestId = guestId;
        locationFacet.timestampMs = time;
        locationFacet.start = time;
        locationFacet.end = time;
        locationFacet.guestId = guestId;

        // Set both api and apiKeyId fields to zero since this location is not coming from a connector
        locationFacet.api = 0;
        locationFacet.apiKeyId = 0L;

        Location ipLocation = null;
        try {
    		ipLocation = geoIpLookupService.getLocation(ipAddress);
        } catch (Throwable t) {
            StringBuilder sb = new StringBuilder("module=web component=guestServiceImpl action=checkIn")
                    .append(" guestId=").append(guestId).append(" message=" + t.getMessage());
            logger.info(sb.toString());
        }
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
		} else if (env.get("ip2location.apiKey")!=null) {
            String ip2locationKey = env.get("ip2location.apiKey");
            String jsonString;
            try {
                jsonString = HttpUtils.fetch("http://api.ipinfodb.com/v3/ip-city/?key=" + ip2locationKey + "&ip=" + ipAddress + "&format=json");
            }
            catch (UnexpectedHttpResponseCodeException e) {
                // simply log the error and don't persist anything to the guest location table
                logger.warn(String.format("ip2location http error; code is %s, message is '%s'", e.getHttpResponseCode(),
                                          e.getHttpResponseMessage()));
                return;
            }
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

    @Override
    public void destroy() throws Exception {
        executor.shutdown();
    }
}
