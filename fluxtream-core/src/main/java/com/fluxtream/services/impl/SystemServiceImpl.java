package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.SystemService;
import com.fluxtream.utils.JPAUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Scope("singleton")
@Transactional(readOnly=true)
public class SystemServiceImpl implements SystemService {

    static final Logger logger = Logger.getLogger(SystemServiceImpl.class);

    @Autowired
    ConnectorUpdateService connectorUpdateService;

	@Autowired
	Configuration env;

	@PersistenceContext
	EntityManager em;

	static Map<String, Connector> scopedApis = new Hashtable<String, Connector>();

    static {
        scopedApis.put("https://www.googleapis.com/auth/latitude.all.best",
                       Connector.getConnector("google_latitude"));
    }

    @Override
	public List<ConnectorInfo> getConnectors() throws Exception {
		List<ConnectorInfo> all = JPAUtils.find(em, ConnectorInfo.class,
				"connectors.all", (Object[]) null);
		if (all.size() == 0) {
			initializeConnectorList();
			all = JPAUtils.find(em, ConnectorInfo.class, "connectors.all",
					(Object[]) null);
		}
		for (ConnectorInfo connectorInfo : all) {
			em.detach(connectorInfo);
			connectorInfo.image = "/" + env.get("release")
					+ connectorInfo.image;
		}
		return all;
	}

    @Override
    public ConnectorInfo getConnectorInfo(final String connectorName) {
        final ConnectorInfo connectorInfo = JPAUtils.findUnique(em, ConnectorInfo.class, "connector.byName", connectorName);
        return connectorInfo;
    }

    private void initializeConnectorList() throws Exception {
		ResourceBundle res = ResourceBundle.getBundle("messages/connectors");
        int order = 0;
        //final String moves = "Moves";
        //String[] movesKeys = checkKeysExist(moves, Arrays.asList("moves.client.id", "moves.client.secret", "foursquare.client.id", "foursquare.client.secret"));
        //final ConnectorInfo movesConnectorInfo = new ConnectorInfo(moves,
        //                                                           "/images/connectors/connector-moves.jpg",
        //                                                           res.getString("moves"),
        //                                                           "/moves/oauth2/token",
        //                                                           Connector.getConnector("moves"), order++, movesKeys!=null,
        //                                                           false, true, movesKeys);
        //em.persist(movesConnectorInfo);
        final String latitude = "Google Latitude";
        String[] latitudeKeys = checkKeysExist(latitude, Arrays.asList("google.client.id", "google.client.secret", "google_latitudeApiKey"));
        final ConnectorInfo latitudeConnectorInfo = new ConnectorInfo(latitude,
                                                                      "/images/connectors/connector-google_latitude.jpg",
                                                                      res.getString("google_latitude"),
                                                                      "/google/oauth2/token?scope=https://www.googleapis.com/auth/latitude.all.best",
                                                                      Connector.getConnector("google_latitude"), order++, latitudeKeys!=null,
                                                                      false, true, latitudeKeys);
        latitudeConnectorInfo.supportsRenewTokens = true;
        latitudeConnectorInfo.renewTokensUrlTemplate = "google/oauth2/%s/token?scope=https://www.googleapis.com/auth/latitude.all.best";
        em.persist(latitudeConnectorInfo);
        final String fitbit = "Fitbit";
        String[] fitbitKeys = checkKeysExist(fitbit, Arrays.asList("fitbitConsumerKey", "fitbitConsumerSecret"));
        em.persist(new ConnectorInfo(fitbit,
                                     "/images/connectors/connector-fitbit.jpg",
                                     res.getString("fitbit"), "/fitbit/token",
                                     Connector.getConnector("fitbit"), order++, fitbitKeys!=null,
                                     false, true, fitbitKeys));
        final String bodyMedia = "BodyMedia";
        String[] bodymediaKeys = checkKeysExist(bodyMedia, Arrays.asList("bodymediaConsumerKey", "bodymediaConsumerSecret"));
        final ConnectorInfo bodymediaConnectorInfo = new ConnectorInfo(bodyMedia,
                                                                       "/images/connectors/connector-bodymedia.jpg",
                                                                       res.getString("bodymedia"),
                                                                       "/bodymedia/token",
                                                                       Connector.getConnector("bodymedia"), order++, bodymediaKeys!=null,
                                                                       false, true, bodymediaKeys);
        bodymediaConnectorInfo.supportsRenewTokens = true;
        bodymediaConnectorInfo.renewTokensUrlTemplate = "bodymedia/token?apiKeyId=%s";
        em.persist(bodymediaConnectorInfo);

        final String withings = "Withings";
        String[] withingsKeys = checkKeysExist(withings, Arrays.asList("withingsConsumerKey", "withingsConsumerSecret", "withings.publickey"));
        em.persist(new ConnectorInfo(withings,
                                     "/images/connectors/connector-withings.jpg",
                                     res.getString("withings"),
                                     "ajax:/withings/enterCredentials",
                                     Connector.getConnector("withings"), order++, withingsKeys!=null,
                                     false, true, withingsKeys));

        final String zeo = "Zeo";
        String[] zeoKeys = checkKeysExist(zeo, Arrays.asList("zeoConsumerKey", "zeoConsumerSecret", "zeoApiKey"));
        em.persist(new ConnectorInfo(zeo,
                                     "/images/connectors/connector-zeo.jpg",
                                     res.getString("zeo"),
                                     "ajax:/zeo/enterCredentials",
                                     Connector.getConnector("zeo"), order++, zeoKeys!=null,
                                     false, true, zeoKeys));
        final String mymee = "Mymee";
        String[] mymeeKeys = checkKeysExist(mymee, new ArrayList<String>());
        em.persist(new ConnectorInfo(mymee,
                                     "/images/connectors/connector-mymee.jpg",
                                     res.getString("mymee"),
                                     "ajax:/mymee/enterFetchURL",
                                     Connector.getConnector("mymee"), order++, mymeeKeys!=null,
                                     false, true, mymeeKeys));
        final String quantifiedMind = "QuantifiedMind";
        String[] quantifiedMindKeys = checkKeysExist(quantifiedMind, new ArrayList<String>());
        em.persist(new ConnectorInfo(quantifiedMind,
                                     "/images/connectors/connector-quantifiedmind.jpg",
                                     res.getString("quantifiedmind"),
                                     "ajax:/quantifiedmind/getTokenDialog",
                                     Connector.getConnector("quantifiedmind"), order++, quantifiedMindKeys!=null,
                                     false, true, quantifiedMindKeys));
        final String flickr = "Flickr";
        String[] flickrKeys = checkKeysExist(flickr, Arrays.asList("flickrConsumerKey", "flickrConsumerSecret", "flickr.validRedirectURL"));
        em.persist(new ConnectorInfo(flickr,
                                     "/images/connectors/connector-flickr.jpg",
                                     res.getString("flickr"),
                                     "/flickr/token",
                                     Connector.getConnector("flickr"), order++, flickrKeys!=null,
                                     false, true, flickrKeys));
        final String googleCalendar = "Google Calendar";
        String[] googleCalendarKeys = checkKeysExist(googleCalendar, Arrays.asList("googleConsumerKey", "googleConsumerSecret"));
        em.persist(new ConnectorInfo(googleCalendar,
                                     "/images/connectors/connector-google_calendar.jpg",
                                     res.getString("google_calendar"),
                                     "/calendar/token",
                                     Connector.getConnector("google_calendar"), order++, googleCalendarKeys!=null,
                                     false, true, googleCalendarKeys));
        final String lastFm = "Last fm";
        String[] lastFmKeys = checkKeysExist(lastFm, Arrays.asList("lastfmConsumerKey", "lastfmConsumerSecret"));
        em.persist(new ConnectorInfo(lastFm,
                                     "/images/connectors/connector-lastfm.jpg",
                                     res.getString("lastfm"),
                                     "/lastfm/token",
                                     Connector.getConnector("lastfm"), order++, lastFmKeys!=null,
                                     false, true, lastFmKeys));
        final String twitter = "Twitter";
        String[] twitterKeys = checkKeysExist(twitter, Arrays.asList("twitterConsumerKey", "twitterConsumerSecret"));
        em.persist(new ConnectorInfo(twitter,
                                     "/images/connectors/connector-twitter.jpg",
                                     res.getString("twitter"), "/twitter/token",
                                     Connector.getConnector("twitter"), order++, twitterKeys!=null,
                                     false, true, twitterKeys));
        final String github = "Github";
        String[] githubKeys = checkKeysExist(github, Arrays.asList("singly.client.id", "singly.client.secret"));
        em.persist(new ConnectorInfo(github,
                                     "/images/connectors/connector-github.jpg",
                                     res.getString("github"),
                                     singlyAuthorizeUrl("github"),
                                     Connector.getConnector("github"), order++, githubKeys!=null,
                                     false, true, githubKeys));
        final String fluxtreamCapture = "Fluxtream Capture";
        String[] fluxtreamCaptureKeys = checkKeysExist(fluxtreamCapture, new ArrayList<String>());
        em.persist(new ConnectorInfo(fluxtreamCapture,
                                     "/images/connectors/connector-fluxtream_capture.png",
                                     res.getString("fluxtream_capture"),
                                     "ajax:/fluxtream_capture/about",
                                     Connector.getConnector("fluxtream_capture"), order++, fluxtreamCaptureKeys!=null,
                                     false, true, fluxtreamCaptureKeys));
        String[] runkeeperKeys = checkKeysExist("Runkeeper", Arrays.asList("runkeeperConsumerKey", "runkeeperConsumerSecret"));
        final String runKeeper = "RunKeeper";
        em.persist(new ConnectorInfo(runKeeper,
                                     "/images/connectors/connector-runkeeper.jpg",
                                     res.getString("runkeeper"),
                                     "/runkeeper/token",
                                     Connector.getConnector("runkeeper"), order, runkeeperKeys!=null,
                                     false, true, runkeeperKeys));
	}

    private String[] checkKeysExist(String connectorName, List<String> keys) {
        String[] checkedKeys = new String[keys.size()];
        int i=0;
        for (String key : keys) {
            String value = env.get(key);
            if (value==null) {
                logger.info("Couldn't find key " + key + " \"" + key + "\" while populating the connector table thus disabling the " + connectorName + " connector");
                return null;
            } else if (value.equals("xxx")) {
                logger.info("No value specified " + key + " \"" + key + "\" while populating the connector table thus disabling the " + connectorName + " connector");
            } else {
                checkedKeys[i++] = key;
            }
        }
        return checkedKeys;
    }

    private String singlyAuthorizeUrl(final String service) {
        return (new StringBuilder("https://api.singly.com/oauth/authorize?client_id=")
            .append(env.get("singly.client.id"))
            .append("&redirect_uri=")
            .append(env.get("homeBaseUrl"))
            .append("singly/")
                    .append(service)
                    .append("/callback")
            .append("&service=")
            .append(service)).toString();
    }

    @Override
	public Connector getApiFromGoogleScope(String scope) {
		return scopedApis.get(scope);
	}

}
