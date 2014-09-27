package org.fluxtream.core.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ConnectorInfo;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.services.SystemService;
import org.fluxtream.core.updaters.quartz.Consumer;
import org.fluxtream.core.updaters.quartz.Producer;
import org.fluxtream.core.utils.JPAUtils;
import net.sf.json.JSONArray;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Scope("singleton")
@Transactional(readOnly=true)
public class SystemServiceImpl implements SystemService, ApplicationListener<ContextRefreshedEvent> {

    static final Logger logger = Logger.getLogger(SystemServiceImpl.class);

    @Autowired
    ConnectorUpdateService connectorUpdateService;

	@Autowired
	Configuration env;

    @Autowired
    GuestService guestService;

	@PersistenceContext
	EntityManager em;

    @Autowired(required=false)
    Producer producer;

    @Autowired(required=false)
    Consumer consumer;

    @Autowired
    JPADaoService jpaDaoService;

	static Map<String, Connector> scopedApis = new Hashtable<String, Connector>();

    static {
        if (Connector.getConnector("google_latitude")!=null)
            scopedApis.put("https://www.googleapis.com/auth/latitude.all.best",
                           Connector.getConnector("google_latitude"));
        if (Connector.getConnector("google_calendar")!=null)
            scopedApis.put("https://www.googleapis.com/auth/calendar.readonly",
                           Connector.getConnector("google_calendar"));
        if (Connector.getConnector("sms_backup")!=null)
            scopedApis.put("https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/gmail.readonly",
                           Connector.getConnector("sms_backup"));
    }

    @Override
    public List<ConnectorInfo> getConnectors() throws Exception {
		List<ConnectorInfo> all = JPAUtils.find(em, ConnectorInfo.class, "connectors.all", (Object[])null);
        // Removed check for initializing the Connector table since this was causing
        // duplication of the entries in the Connector table.  This means that we may
        // end up returning an incomplete list of connectors if this is
        // called during a thread other than the one in onApplicationEvent during startup
		//if (all.size() == 0) {
		//	resetConnectorList();
		//	all = JPAUtils.find(em, ConnectorInfo.class, "connectors.all",
		//			(Object[]) null);
		//}
		return all;
	}

    @Override
    public ConnectorInfo getConnectorInfo(final String connectorName)  throws Exception  {
        //List<ConnectorInfo> all = JPAUtils.find(em, ConnectorInfo.class, "connectors.all", (Object[])null);
        // Removed check for initializing the Connector table since this was causing
        // duplication of the entries in the Connector table.  This means that we may
        // end up returning an incomplete list of connectors if this is
        // called during a thread other than the one in onApplicationEvent during startup
        //if (all.size() == 0) {
        //    resetConnectorList();
        //}
        final ConnectorInfo connectorInfo = JPAUtils.findUnique(em, ConnectorInfo.class, "connector.byName", connectorName);
        return connectorInfo;
    }

    @Transactional(readOnly = false)
    private void initializeConnectorList() {
		ResourceBundle res = ResourceBundle.getBundle("messages/connectors");
        int order = 0;
        String release = env.get("release");

        final String jawboneUp = "Jawbone UP";
        String[] jawboneUpKeys = checkKeysExist(jawboneUp, Arrays.asList("jawboneUp.client.id", "jawboneUp.client.secret", "jawboneUp.validRedirectURL"));
        final ConnectorInfo jawboneUpConnectorInfo = new ConnectorInfo(jawboneUp,
                                                                      "/" + release + "/images/connectors/connector-up.png",
                                                                      res.getString("up"),
                                                                      "/up/token",
                                                                      Connector.getConnector("up"), order++, jawboneUpKeys!=null,
                                                                      false, true, jawboneUpKeys);
        jawboneUpConnectorInfo.supportsRenewTokens = true;
        jawboneUpConnectorInfo.renewTokensUrlTemplate = "up/token?apiKeyId=%s";
        em.persist(jawboneUpConnectorInfo);

        final String evernote = "Evernote";
        String[] evernoteKeys = checkKeysExist(evernote, Arrays.asList("evernoteConsumerKey", "evernoteConsumerSecret", "evernote.sandbox"));
        final ConnectorInfo evernoteConnectorInfo = new ConnectorInfo(evernote,
                                                                      "/" + release + "/images/connectors/connector-evernote.jpg",
                                                                      res.getString("evernote"),
                                                                      "/evernote/token",
                                                                      Connector.getConnector("evernote"), order++, evernoteKeys!=null,
                                                                      false, true, evernoteKeys);
        evernoteConnectorInfo.supportsRenewTokens = true;
        evernoteConnectorInfo.renewTokensUrlTemplate = "evernote/token?apiKeyId=%s";
        em.persist(evernoteConnectorInfo);

        final String facebook = "Facebook";
        String[] facebookKeys = checkKeysExist(facebook, Arrays.asList("facebook.appId", "facebook.appSecret"));
        final ConnectorInfo facebookConnectorInfo = new ConnectorInfo(facebook,
                                                                      "/" + release + "/images/connectors/connector-facebook.jpg",
                                                                   res.getString("facebook"),
                                                                   "/facebook/token",
                                                                   Connector.getConnector("facebook"), order++, facebookKeys!=null,
                                                                   false, false, facebookKeys);
        em.persist(facebookConnectorInfo);
        final String moves = "Moves";
        String[] movesKeys = checkKeysExist(moves, Arrays.asList("moves.client.id", "moves.client.secret", "moves.validRedirectURL", "foursquare.client.id", "foursquare.client.secret"));
        final ConnectorInfo movesConnectorInfo = new ConnectorInfo(moves,
                                                                   "/" + release + "/images/connectors/connector-moves.jpg",
                                                                   res.getString("moves"),
                                                                   "/moves/oauth2/token",
                                                                   Connector.getConnector("moves"), order++, movesKeys!=null,
                                                                   false, true, movesKeys);

        movesConnectorInfo.supportsRenewTokens = true;
        movesConnectorInfo.renewTokensUrlTemplate = "moves/oauth2/token?apiKeyId=%s";
        em.persist(movesConnectorInfo);
        final String latitude = "Google Latitude";
        String[] latitudeKeys = checkKeysExist(latitude, Arrays.asList("google.client.id", "google.client.secret"));
        final ConnectorInfo latitudeConnectorInfo = new ConnectorInfo(latitude,
                                                                      "/" + release + "/images/connectors/connector-google_latitude.jpg",
                                                                      res.getString("google_latitude"),
                                                                      "upload:google_latitude",
                                                                      Connector.getConnector("google_latitude"), order++, latitudeKeys!=null,
                                                                      true, false, latitudeKeys);
        latitudeConnectorInfo.supportsRenewTokens = false;
        latitudeConnectorInfo.renewTokensUrlTemplate = "google/oauth2/%s/token?scope=https://www.googleapis.com/auth/latitude.all.best";
        em.persist(latitudeConnectorInfo);
        final String fitbit = "Fitbit";
        String[] fitbitKeys = checkKeysExist(fitbit, Arrays.asList("fitbitConsumerKey", "fitbitConsumerSecret"));
        final ConnectorInfo fitbitConnectorInfo = new ConnectorInfo(fitbit, "/images/connectors/connector-fitbit.jpg", res.getString("fitbit"), "/fitbit/token", Connector.getConnector("fitbit"), order++, fitbitKeys != null, false, true, fitbitKeys);
        fitbitConnectorInfo.supportsRenewTokens = true;
        fitbitConnectorInfo.renewTokensUrlTemplate = "fitbit/token?apiKeyId=%s";
        em.persist(fitbitConnectorInfo);
        final String bodyMedia = "BodyMedia";
        String[] bodymediaKeys = checkKeysExist(bodyMedia, Arrays.asList("bodymediaConsumerKey", "bodymediaConsumerSecret"));
        final ConnectorInfo bodymediaConnectorInfo = new ConnectorInfo(bodyMedia,
                                                                       "/" + release + "/images/connectors/connector-bodymedia.jpg",
                                                                       res.getString("bodymedia"),
                                                                       "/bodymedia/token",
                                                                       Connector.getConnector("bodymedia"), order++, bodymediaKeys!=null,
                                                                       false, true, bodymediaKeys);
        bodymediaConnectorInfo.supportsRenewTokens = true;
        bodymediaConnectorInfo.renewTokensUrlTemplate = "bodymedia/token?apiKeyId=%s";
        em.persist(bodymediaConnectorInfo);

        final String withings = "Withings";
        String[] withingsKeys = checkKeysExist(withings, Arrays.<String>asList("withingsConsumerKey", "withingsConsumerSecret"));
        final ConnectorInfo withingsConnectorInfo = new ConnectorInfo(
                withings, "/" + release + "/images/connectors/connector-withings.jpg",
                res.getString("withings"), "/withings/token",
                Connector.getConnector("withings"), order++, withingsKeys != null,
                false, true, withingsKeys);
        withingsConnectorInfo.supportsRenewTokens = true;
        withingsConnectorInfo.renewTokensUrlTemplate = "withings/token?apiKeyId=%s";
        em.persist(withingsConnectorInfo);

        final String zeo = "Zeo";
        String[] zeoKeys = checkKeysExist(zeo, new ArrayList<String>());
        // Zeo no longer supports sync.  The myzeo servers were disabled due to bankruptcy in May/June 2013
        em.persist(new ConnectorInfo(zeo,
                                     "/" + release + "/images/connectors/connector-zeo.jpg",
                                     res.getString("zeo"),
                                     "ajax:/zeo/enterCredentials",
                                     Connector.getConnector("zeo"), order++, zeoKeys!=null,
                                     false, false, zeoKeys));
        final String mymee = "Mymee";
        em.persist(new ConnectorInfo(mymee,
                                     "/" + release + "/images/connectors/connector-mymee.jpg",
                                     res.getString("mymee"),
                                     "ajax:/mymee/enterAuthInfo",
                                     Connector.getConnector("mymee"), order++, true,
                                     false, true, null));
        final String quantifiedMind = "QuantifiedMind";
        String[] quantifiedMindKeys = checkKeysExist(quantifiedMind, new ArrayList<String>());
        em.persist(new ConnectorInfo(quantifiedMind,
                                     "/" + release + "/images/connectors/connector-quantifiedmind.jpg",
                                     res.getString("quantifiedmind"),
                                     "ajax:/quantifiedmind/getTokenDialog",
                                     Connector.getConnector("quantifiedmind"), order++, quantifiedMindKeys!=null,
                                     false, true, quantifiedMindKeys));
        final String flickr = "Flickr";
        String[] flickrKeys = checkKeysExist(flickr, Arrays.asList("flickrConsumerKey", "flickrConsumerSecret", "flickr.validRedirectURL"));
        final ConnectorInfo flickrConnectorInfo = new ConnectorInfo(flickr, "/" + release + "/images/connectors/connector-flickr.jpg", res.getString("flickr"), "/flickr/token", Connector.getConnector("flickr"), order++, flickrKeys != null, false, true, flickrKeys);
        flickrConnectorInfo.supportsRenewTokens = true;
        flickrConnectorInfo.renewTokensUrlTemplate = "flickr/token?apiKeyId=%s";
        em.persist(flickrConnectorInfo);
        final String googleCalendar = "Google Calendar";
        String[] googleCalendarKeys = checkKeysExist(googleCalendar, Arrays.asList("google.client.id", "google.client.secret"));
        final ConnectorInfo googleCalendarConnectorInfo =
                new ConnectorInfo(googleCalendar,
                                  "/" + release + "/images/connectors/connector-google_calendar.jpg",
                                  res.getString("google_calendar"),
                                  "/google/oauth2/token?scope=https://www.googleapis.com/auth/calendar.readonly",
                                  Connector.getConnector("google_calendar"),
                                  order++, googleCalendarKeys != null, false, true, googleCalendarKeys);
        googleCalendarConnectorInfo.supportsRenewTokens = true;
        googleCalendarConnectorInfo.renewTokensUrlTemplate = "google/oauth2/%s/token?scope=https://www.googleapis.com/auth/calendar.readonly";
        em.persist(googleCalendarConnectorInfo);
        final String lastFm = "Last fm";
        String[] lastFmKeys = checkKeysExist(lastFm, Arrays.asList("lastfmConsumerKey", "lastfmConsumerSecret"));
        final ConnectorInfo lastfmConnectorInfo = new ConnectorInfo(lastFm, "/" + release + "/images/connectors/connector-lastfm.jpg", res.getString("lastfm"), "/lastfm/token", Connector.getConnector("lastfm"), order++, lastFmKeys != null, false, true, lastFmKeys);
        lastfmConnectorInfo.supportsRenewTokens = true;
        lastfmConnectorInfo.renewTokensUrlTemplate = "lastfm/token?apiKeyId=%s";
        em.persist(lastfmConnectorInfo);
        final String twitter = "Twitter";
        String[] twitterKeys = checkKeysExist(twitter, Arrays.asList("twitterConsumerKey", "twitterConsumerSecret"));
        ConnectorInfo twitterConnectorInfo = new ConnectorInfo(twitter,
                                     "/" + release + "/images/connectors/connector-twitter.jpg",
                                     res.getString("twitter"), "/twitter/token",
                                     Connector.getConnector("twitter"), order++, twitterKeys!=null,
                                     false, true, twitterKeys);
        twitterConnectorInfo.supportsRenewTokens = true;
        twitterConnectorInfo.renewTokensUrlTemplate = "twitter/token?apiKeyId=%s";
        em.persist(twitterConnectorInfo);
        final String fluxtreamCapture = "Fluxtream Capture";
        String[] fluxtreamCaptureKeys = checkKeysExist(fluxtreamCapture, new ArrayList<String>());
        em.persist(new ConnectorInfo(fluxtreamCapture,
                                     "/" + release + "/images/connectors/connector-fluxtream_capture.png",
                                     res.getString("fluxtream_capture"),
                                     "ajax:/fluxtream_capture/about",
                                     Connector.getConnector("fluxtream_capture"), order++, fluxtreamCaptureKeys!=null,
                                     false, true, fluxtreamCaptureKeys));
        String[] runkeeperKeys = checkKeysExist("Runkeeper", Arrays.asList("runkeeperConsumerKey", "runkeeperConsumerSecret"));
        final String runKeeper = "RunKeeper";
        final ConnectorInfo runkeeperConnectorInfo = new ConnectorInfo(runKeeper, "/" + release + "/images/connectors/connector-runkeeper.jpg", res.getString("runkeeper"), "/runkeeper/token", Connector.getConnector("runkeeper"), order++, runkeeperKeys != null, false, true, runkeeperKeys);
        runkeeperConnectorInfo.supportsRenewTokens = true;
        runkeeperConnectorInfo.renewTokensUrlTemplate = "runkeeper/token?apiKeyId=%s";
        em.persist(runkeeperConnectorInfo);
        String[] smsBackupKeys = checkKeysExist("SMS_Backup", Arrays.asList("google.client.id", "google.client.secret"));
        ConnectorInfo SMSBackupInfo = new ConnectorInfo("SMS_Backup",
                                                        "/" + release + "/images/connectors/connector-sms_backup.jpg",
                                                        res.getString("sms_backup"),
                                                        "/google/oauth2/token?scope=https://www.googleapis.com/auth/userinfo.email%20https://www.googleapis.com/auth/gmail.readonly",
                                                        Connector.getConnector("sms_backup"), order++, true,
                                                        false,true,smsBackupKeys);
        SMSBackupInfo.supportsRenewTokens = true;
        SMSBackupInfo.renewTokensUrlTemplate = "google/oauth2/%s/token?scope=https://www.googleapis.com/auth/userinfo.email%%20https://www.googleapis.com/auth/gmail.readonly";
        em.persist(SMSBackupInfo);
	}

    @Transactional(readOnly = false)
    private String[] checkKeysExist(String connectorName, List<String> keys) {
        String[] checkedKeys = new String[keys.size()];
        int i=0;
        boolean fatalMissingKey=false;
        boolean nonFatalMissingKey=false;

        for (String key : keys) {
            String value = env.get(key);
            if (value==null) {
                fatalMissingKey=true;
                String msg = "Couldn't find key \"" + key + "\" while initializing the connector table.  You need to add that key to your properties files.\n" +
                        "  See fluxtream-web/src/main/resources/samples/oauth.properties for details.";
                logger.info(msg);
                System.out.println(msg);
            } else if (value.equals("xxx")) {
                nonFatalMissingKey=true;
                String msg = "**** Found key \"" + key + "=xxx\" while populating the connector table.  Disabling the " + connectorName + " connector";
                logger.info(msg);
                System.out.println(msg);
            } else {
                checkedKeys[i++] = key;
            }
        }

        if(fatalMissingKey) {
            String msg = "***** Exiting execution due to missing configuration keys. See fluxtream-web/src/main/resources/samples/oauth.properties for details.";
            logger.info(msg);
            System.out.println(msg);
            System.exit(-1);
        }
        else if(nonFatalMissingKey) {
            return null;
        }
        return checkedKeys;
    }

    //private String singlyAuthorizeUrl(final String service) {
    //    return (new StringBuilder("https://api.singly.com/oauth/authorize?client_id=")
    //        .append(env.get("singly.client.id"))
    //        .append("&redirect_uri=")
    //        .append(env.get("homeBaseUrl"))
    //        .append("singly/")
    //                .append(service)
    //                .append("/callback")
    //        .append("&service=")
    //        .append(service)).toString();
    //}

    @Override
	public Connector getApiFromGoogleScope(String scope) {
		return scopedApis.get(scope);
	}

    @Transactional(readOnly = false)
    public void resetConnectorList() throws Exception {
        System.out.println("Resetting connector table");
        // Clear the existing data out of the Connector table
        JPAUtils.execute(em,"connector.deleteAll");
        // The following call will initialize the Connector table by calling
        // the initializeConnectorList function and return the result
        initializeConnectorList();
    }

    @Transactional(readOnly = false)
    public boolean checkConnectorInstanceKeys(List<ConnectorInfo> connectors)
    {
        // For each connector type in connectorInfos which is enabled, make sure that all of the existing connector
        // instances have stored apiKeyAttributeKeys.  This is to support safe migration to version 0.9.0017.
        // Prior versions relied to continued coherence between the keys in the properties files
        // in fluxtream-web/src/main/resources and the existing connector instances.  However, that behavior
        // conflicted with migrating a given machine to a different host name or migrating a given DB to a
        // different server without breaking sync capability for existing connector instances.
        //
        // The new behavior stores the apiKeyAttributeKeys from the properties file in the ApiKeyAttribute
        // table for each connector instance, which makes it more portable but also incurrs a migration
        // requirement.  This function checks whether that migration needs to be performed for a given DB
        // instance
        JSONArray connectorsArray = new JSONArray();
        boolean missingKeys=false;

        for (int i = 0; i < connectors.size(); i++) {
            final ConnectorInfo connectorInfo = connectors.get(i);
            final Connector api = connectorInfo.getApi();
            if (api == null) {
                StringBuilder sb = new StringBuilder("module=SystemServiceImpl component=connectorStore action=checkConnectorInstanceKeys ")
                        .append("message=\"null connector for " + connectorInfo.getName() + "\"");
                logger.warn(sb.toString());
                continue;
            }
            if(connectorInfo.enabled==false) {
                StringBuilder sb = new StringBuilder("module=SystemServiceImpl component=connectorStore action=checkConnectorInstanceKeys ")
                        .append("message=\"skipping connector instance keys check for disabled connector" + connectorInfo.getName() + "\"");
                logger.info(sb.toString());
                continue;
            }
            String[] apiKeyAttributeKeys = connectorInfo.getApiKeyAttributesKeys();
            if(apiKeyAttributeKeys==null) {
                StringBuilder sb = new StringBuilder("module=SystemServiceImpl component=connectorStore action=checkConnectorInstanceKeys ")
                        .append("message=\"skipping connector instance keys check for connector" + connectorInfo.getName() + "; does not use keys\"");
                logger.info(sb.toString());
                continue;
            }
            // This connector type is enabled, find all the instance keys for this connector type
            List<ApiKey> apiKeys = JPAUtils.find(em, ApiKey.class, "apiKeys.all.byApi", api.value());
            for(ApiKey apiKey: apiKeys) {
                StringBuilder sb = new StringBuilder("module=SystemServiceImpl component=connectorStore action=checkConnectorInstanceKeys apiKeyId=" + apiKey.getId())
                                .append(" message=\"checking connector instance keys for connector" + connectorInfo.getName() + "\"");

                logger.info(sb.toString());

                // Iterate over the apiKeyAttributeKeys to check if each is present
                for(String apiKeyAttributeKey: apiKeyAttributeKeys) {
                    String apiKeyAttributeValue = guestService.getApiKeyAttribute(apiKey, apiKeyAttributeKey);
                    if(apiKeyAttributeValue==null) {
                        missingKeys=true;
                        String msg = "**** Missing key \"" + apiKeyAttributeKey + "\" for apiKeyId=" + apiKey.getId() + " api=" + api.value()
                                ;
                        StringBuilder sb2 = new StringBuilder("module=SystemServiceImpl component=connectorStore action=checkConnectorInstanceKeys apiKeyId=" + apiKey.getId())
			    .append(" message=\"").append(msg).append("\"");
                        logger.info(sb2.toString());
                        System.out.println(msg);
                    }
                }
            }

        }
        return missingKeys;
    }

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        System.out.println("ApplicationContext started");
        if (env.get("apiWebApp")!=null) {
            System.out.println("This is the API web app... Connector list isn't needed. Bye.");
            return;
        }
        if (event.getApplicationContext().getDisplayName().equals("Root WebApplicationContext")) {
            try {
                resetConnectorList();
                resetSynchingApiKeys();
                List<ConnectorInfo> connectors = getConnectors();
                boolean missingKeys=checkConnectorInstanceKeys(connectors);

                if(missingKeys) {
                    String msg = "***** Exiting execution due to missing connector instance keys.\n  Check out fluxtream-admin-tools project, build, and execute 'java -jar target/flx-admin-tools.jar 5'";
                    List<ConnectorInfo> connectors2 = getConnectors();
                    System.out.println("List of Connector table: before=" + connectors.size() + ", after=" + connectors2.size());

                    logger.info(msg);
                    System.out.println(msg);
                    System.exit(-1);
                }
                consumer.setContextStarted();
                producer.setContextStarted();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void resetSynchingApiKeys() {
        jpaDaoService.execute("UPDATE ApiKey apiKey SET apiKey.synching=false");
    }

}
