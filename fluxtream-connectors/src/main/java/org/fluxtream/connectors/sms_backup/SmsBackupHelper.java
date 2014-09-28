package org.fluxtream.connectors.sms_backup;

import javax.mail.MessagingException;
import javax.mail.Store;

import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.MailUtils;

public class SmsBackupHelper {

	String username, password;
	
	public SmsBackupHelper(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public static boolean checkAuthorization(GuestService guestService, long guestId) {
		ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector("SMS_BACKUP"));
		return apiKey!=null;
	}

	public boolean testConnection() throws MessagingException {
		Store gmailImapStore = MailUtils.getGmailImapStore(username, password);
		return gmailImapStore!=null;
	}
	
}
