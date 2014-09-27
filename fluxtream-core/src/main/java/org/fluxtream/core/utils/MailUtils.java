package org.fluxtream.core.utils;

import java.security.Provider;
import java.security.Security;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import org.fluxtream.core.utils.sasl.OAuth2SaslClientFactory;
import com.sun.mail.imap.IMAPSSLStore;

public class MailUtils {

	public static Store getGmailImapStore(String emailuser, String emailpassword)
			throws MessagingException {
		Session session;
		String emailserver = "imap.gmail.com";
		String emailprovider = "imaps";
		Store store = null;

		Properties props = System.getProperties();
		props.setProperty("mail.pop3s.rsetbeforequit", "true");
		props.setProperty("mail.pop3.rsetbeforequit", "true");
		props.setProperty("mail.imaps.port", "993");
		props.setProperty("mail.imaps.host", "imap.gmail.com");
		props.setProperty("mail.store.protocol", "imaps");
		props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.setProperty("mail.imap.socketFactory.fallback", "false");

		session = Session.getInstance(props, null);

		store = session.getStore(emailprovider);
		store.connect(emailserver, emailuser, emailpassword);
		return store;
	}

    public static final class OAuth2Provider extends Provider {
        private static final long serialVersionUID = 1L;
        public OAuth2Provider() {
            super("Google OAuth2 Provider", 1.0,
                  "Provides the XOAUTH2 SASL Mechanism");
            put("SaslClientFactory.XOAUTH2",
                "org.fluxtream.utils.sasl.OAuth2SaslClientFactory");
        }
    }

    static {
        Security.addProvider(new OAuth2Provider());
    }

    public static Store getGmailImapStoreViaSASL(final String emailAddress, final String accessToken) throws MessagingException{
        Properties props = new Properties();
        props.put("mail.imaps.sasl.enable", "true");
        props.put("mail.imaps.sasl.mechanisms", "XOAUTH2");
        props.put(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, accessToken);


        Session session = Session.getInstance(props);
        IMAPSSLStore store = new IMAPSSLStore(session, null);
        store.connect("imap.gmail.com", 993, emailAddress, "");
        return store;
    }
}
