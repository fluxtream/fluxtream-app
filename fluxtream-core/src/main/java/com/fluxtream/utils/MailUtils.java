package com.fluxtream.utils;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

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
		props.setProperty("mail.imap.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		props.setProperty("mail.imap.socketFactory.fallback", "false");

		session = Session.getInstance(props, null);

		store = session.getStore(emailprovider);
		store.connect(emailserver, emailuser, emailpassword);
		return store;
	}
	
}
