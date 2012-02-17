package com.fluxtream.connectors.sms_backup;

import javax.mail.MessagingException;
import javax.mail.Store;

import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.MailUtils;

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
