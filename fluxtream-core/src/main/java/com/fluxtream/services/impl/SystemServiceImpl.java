package com.fluxtream.services.impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Scope("singleton")
@Transactional(readOnly=true)
public class SystemServiceImpl implements SystemService {

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
	public List<ConnectorInfo> getConnectors() {
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

    private void initializeConnectorList() {
		ResourceBundle res = ResourceBundle.getBundle("messages/connectors");
        int order = 0;
        final ConnectorInfo movesConnectorInfo = new ConnectorInfo("Moves",
                                                                   "/images/connectors/connector-moves.jpg",
                                                                   res.getString("moves"),
                                                                   "/moves/oauth2/token",
                                                                   Connector.getConnector("moves"), order++, true);
        em.persist(movesConnectorInfo);
        final ConnectorInfo latitudeConnectorInfo = new ConnectorInfo("Google Latitude",
                                                                      "/images/connectors/connector-google_latitude.jpg",
                                                                      res.getString("google_latitude"),
                                                                      "/google/oauth2/token?scope=https://www.googleapis.com/auth/latitude.all.best",
                                                                      Connector.getConnector("google_latitude"), order++, true);
        latitudeConnectorInfo.supportsRenewTokens = true;
        latitudeConnectorInfo.renewTokensUrlTemplate = "google/oauth2/%s/token?scope=https://www.googleapis.com/auth/latitude.all.best";
        em.persist(latitudeConnectorInfo);
        em.persist(new ConnectorInfo("Fitbit",
                                     "/images/connectors/connector-fitbit.jpg",
                                     res.getString("fitbit"), "/fitbit/token",
                                     Connector.getConnector("fitbit"), order++, true));
        final ConnectorInfo bodymediaConnectorInfo = new ConnectorInfo("BodyMedia",
                                                                       "/images/connectors/connector-bodymedia.jpg",
                                                                       res.getString("bodymedia"),
                                                                       "/bodymedia/token",
                                                                       Connector.getConnector("bodymedia"), order++, true);
        bodymediaConnectorInfo.supportsRenewTokens = true;
        bodymediaConnectorInfo.renewTokensUrlTemplate = "bodymedia/token?apiKeyId=%s";
        em.persist(bodymediaConnectorInfo);
        em.persist(new ConnectorInfo("Withings",
                                     "/images/connectors/connector-withings.jpg",
                                     res.getString("withings"),
                                     "ajax:/withings/enterCredentials",
                                     Connector.getConnector("withings"), order++, true));
        em.persist(new ConnectorInfo("Zeo",
                                     "/images/connectors/connector-zeo.jpg",
                                     res.getString("zeo"),
                                     "ajax:/zeo/enterCredentials",
                                     Connector.getConnector("zeo"), order++, true));
        em.persist(new ConnectorInfo("Mymee",
                                     "/images/connectors/connector-mymee.jpg",
                                     res.getString("mymee"),
                                     "ajax:/mymee/enterFetchURL",
                                     Connector.getConnector("mymee"), order++, true));
        em.persist(new ConnectorInfo("QuantifiedMind",
                                     "/images/connectors/connector-quantifiedmind.jpg",
                                     res.getString("quantifiedmind"),
                                     "ajax:/quantifiedmind/getTokenDialog",
                                     Connector.getConnector("quantifiedmind"), order++, true));
        em.persist(new ConnectorInfo("Flickr",
                                     "/images/connectors/connector-flickr.jpg",
                                     res.getString("flickr"),
                                     "/flickr/token",
                                     Connector.getConnector("flickr"), order++, true));
        em.persist(new ConnectorInfo("Google Calendar",
                                     "/images/connectors/connector-google_calendar.jpg",
                                     res.getString("google_calendar"),
                                     "/calendar/token",
                                     Connector.getConnector("google_calendar"), order++, true));
        em.persist(new ConnectorInfo("Last fm",
                                     "/images/connectors/connector-lastfm.jpg",
                                     res.getString("lastfm"),
                                     "/lastfm/token",
                                     Connector.getConnector("lastfm"), order++, true));
        em.persist(new ConnectorInfo("Twitter",
                                     "/images/connectors/connector-twitter.jpg",
                                     res.getString("twitter"), "/twitter/token",
                                     Connector.getConnector("twitter"), order++, true));
        em.persist(new ConnectorInfo("Github",
                                     "/images/connectors/connector-github.jpg",
                                     res.getString("github"),
                                     singlyAuthorizeUrl("github"),
                                     Connector.getConnector("github"), order++, false));
        em.persist(new ConnectorInfo("Fluxtream Capture",
                                     "/images/connectors/connector-fluxtream_capture.png",
                                     res.getString("fluxtream_capture"),
                                     "ajax:/fluxtream_capture/about",
                                     Connector.getConnector("fluxtream_capture"), order++, true));
        em.persist(new ConnectorInfo("RunKeeper",
                                     "/images/connectors/connector-runkeeper.jpg",
                                     res.getString("runkeeper"),
                                     "/runkeeper/token",
                                     Connector.getConnector("runkeeper"), order++, true));
        em.persist(new ConnectorInfo("SMS Backup",
                                     "/images/connectors/connector-sms_backup.jpg",
                                     res.getString("sms_backup"),
                                     "ajax:/smsBackup/enterCredentials",
                                     Connector.getConnector("sms_backup"), order, true));
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
