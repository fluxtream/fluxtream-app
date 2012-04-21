package com.fluxtream.services.impl;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.services.SystemService;
import com.fluxtream.utils.JPAUtils;

@Transactional(readOnly = true)
@Service
@Scope("singleton")
public class SystemServiceImpl implements SystemService {

	@Autowired
	Configuration env;

	@PersistenceContext
	EntityManager em;

	static Map<String, Connector> scopedApis = new Hashtable<String, Connector>();

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

	// private String moreLink(Connector api) {
	// String moreLink = "<a href='javascript:connectorDescription(\""
	// + api.toString() + "\")'>more...</a>";
	// return moreLink;
	// }

	private void initializeConnectorList() {
		ResourceBundle res = ResourceBundle.getBundle("messages/connectors");
		em.persist(new ConnectorInfo("OpenPath",
				"/images/connectors/connector-openpath.jpg", res.getString("openpath"),
				"ajax:/openPath/enterCredentials", Connector.getConnector("openpath"), 0, true));
		em.persist(new ConnectorInfo("Toodledo",
				"/images/connectors/connector-toodledo.png", res
						.getString("toodledo"),
				"ajax:/toodledo/enterCredentials", Connector
						.getConnector("toodledo"), 1, true));
		em.persist(new ConnectorInfo("BodyTrack",
				"/images/connectors/connector-bodytrack.png", res
						.getString("bodytrack"),
				"ajax:/bodytrack/enterCredentials", Connector
						.getConnector("bodytrack"), 2, true));
		em.persist(new ConnectorInfo("Zeo",
				"/images/connectors/connector-focus-zeo.jpg", res
						.getString("zeo"), "/zeo/subscribe", Connector
						.getConnector("zeo"), 3, true));
		em.persist(new ConnectorInfo("Withings B Scale",
				"/images/connectors/connector-focus-whithings-balance.jpg", res
						.getString("withings"),
				"ajax:/withings/enterCredentials", Connector
						.getConnector("WITHINGS"), 4, true));
		em.persist(new ConnectorInfo("Google Calendar",
				"/images/connectors/connector-google-calendar.jpg", res
						.getString("google_calendar"), "/calendar/token",
				Connector.getConnector("google_calendar"), 5, true));
		// em.persist(new ConnectorInfo("Withings BPM",
		// "/images/connectors/connector-focus-whithings.jpg", "Text " +
		// moreLink(Connector.getConnector("WITHINGS")),
		// "ajax:/withings/enterCredentials",
		// Connector.getConnector("WITHINGS"), 4));
		em.persist(new ConnectorInfo("Fitbit",
				"/images/connectors/connector-focus-fitbit.jpg", res
						.getString("fitbit"), "/fitbit/token", Connector
						.getConnector("fitbit"), 7, true));
		// em.persist(new ConnectorInfo("Freshbooks",
		// "/images/connectors/connector-freshbooks.jpeg",
		// res.getString("freshbooks"), "/freshbooks/token",
		// Connector.getConnector("FRESHBOOKS"), 6));
		// em.persist(new ConnectorInfo("Google Contacts",
		// "/images/connectors/connector-google-contact.jpeg",
		// res.getString("google_contacts"),
		// "/contacts/token", Connector.getConnector("google_contacts"), 7));
		// em.persist(new ConnectorInfo("Picasa",
		// "/images/connectors/connector-picasa.jpg",
		// res.getString("picasa"),
		// "/google/oauth2/token?scope=http://picasaweb.google.com/data/",
		// Api.fromValue("PICASA"), 8));
		em.persist(new ConnectorInfo("Picasa",
				"/images/connectors/connector-picasa.jpg", res
						.getString("picasa"), "/picasa/token", Connector
						.getConnector("picasa"), 8, true));
		em.persist(new ConnectorInfo("Google Latitude",
				"/images/connectors/connector-focus-google_latitude1.jpg", res
						.getString("google_latitude"),
				"/google_latitude/token", Connector
						.getConnector("google_latitude"), 9, true));
		em.persist(new ConnectorInfo("Last fm",
				"/images/connectors/connector-lastfm.jpeg", res
						.getString("lastfm"), "/lastfm/token", Connector
						.getConnector("LASTFM"), 10, true));
		em.persist(new ConnectorInfo("SMS Backup",
				"/images/connectors/connector-smsbackup.jpg", res
						.getString("sms_backup"),
				"ajax:/smsBackup/enterCredentials", Connector
						.getConnector("SMS_BACKUP"), 11, true));
		em.persist(new ConnectorInfo("Twitter",
				"/images/connectors/connector-twitter.jpeg", res
						.getString("twitter"), "/twitter/token", Connector
						.getConnector("twitter"), 12, true));
		String flickrDesc = res.getString("flickr");
		em.persist(new ConnectorInfo("Flickr",
				"/images/connectors/connector-flickr.jpeg", flickrDesc,
				"/flickr/token", Connector.getConnector("flickr"), 13, false));
		em.persist(new ConnectorInfo("BodyMedia",
				"/images/connectors/connector-bodymedia.jpg", res.getString("bodymedia"),
				"/bodymedia/token", Connector.getConnector("bodymedia"), 14, true));
		// em.persist(new ConnectorInfo("Dropbox",
		// "/images/connectors/connector-dropbox.jpg",
		// res.getString("dropbox"),
		// "/dropbox/token", Connector.getConnector("DROPBOX"), 14));
		// em.persist(new ConnectorInfo("Instagram",
		// "/images/connectors/connector-instagram.jpg",
		// res.getString("instagram"),
		// "/instagram/token", Connector.getConnector("instagram"), 15,
		// false));

		// em.persist(new ConnectorInfo("LinkedIn",
		// "/images/connectors/connector-linkedin.jpg",
		// res.getString("linkedin"),
		// "/linkedin/token", Connector.getConnector("linkedin"), 16,
		// false));
		// em.persist(new ConnectorInfo("Khan Academy",
		// "/images/connectors/connector-khanacademy.jpg",
		// res.getString("khanacademy"),
		// "/khanacademy/token", Connector.getConnector("khanacademy"),
		// 17, false));
		// em.persist(new ConnectorInfo("Foursquare",
		// "/images/connectors/connector-foursquare.jpg",
		// res.getString("foursquare"),
		// "/foursquare/token", Connector.getConnector("foursquare"), 18,
		// false));
		// em.persist(new ConnectorInfo("Gowalla",
		// "/images/connectors/connector-gowalla.jpg", res.getString("gowalla"),
		// "/gowalla/token", Connector.getConnector("gowalla"), 19, false));
		// em.persist(new ConnectorInfo("Github",
		// "/images/connectors/connector-github.jpg",
		// res.getString("github"),
		// "/github/token", Connector.getConnector("github"), 20, false));
		// em.persist(new ConnectorInfo("Nike +",
		// "/images/connectors/connector-nikeplus.jpg",
		// res.getString("nikeplus"),
		// "ajax:/nikeplus/enterUsername", Connector
		// .getConnector("nikeplus"), 21, false));
		// em.persist(new ConnectorInfo("Facebook",
		// "/images/connectors/connector-facebook.jpg",
		// res.getString("facebook"),
		// "/facebook/token", Connector.getConnector("facebook"), 22,
		// false));
	}

	@Override
	public Connector getApiFromGoogleScope(String scope) {
		return scopedApis.get(scope);
	}

}
