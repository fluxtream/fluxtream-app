package org.fluxtream.connectors.sms_backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.FolderNotFoundException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.SentDateTerm;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.RateLimitReachedException;
import org.fluxtream.core.connectors.updaters.SettingsAwareUpdater;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.ApiDataService.FacetModifier;
import org.fluxtream.core.services.ApiDataService.FacetQuery;
import org.fluxtream.core.services.SettingsService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.BodyTrackHelper.ChannelStyle;
import org.fluxtream.core.services.impl.BodyTrackHelper.MainTimespanStyle;
import org.fluxtream.core.services.impl.BodyTrackHelper.TimespanStyle;
import org.fluxtream.core.utils.MailUtils;
import org.fluxtream.core.utils.Utils;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.ibm.icu.util.StringTokenizer;
import com.sun.mail.util.BASE64DecoderStream;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "SMS_Backup", value = 6, objectTypes = {
		CallLogEntryFacet.class, SmsEntryFacet.class }, settings=SmsBackupSettings.class,
         defaultChannels = {"sms_backup.call_log"})
public class SmsBackupUpdater extends AbstractUpdater implements SettingsAwareUpdater {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Autowired
    SettingsService settingsService;

    @Autowired
    Configuration env;

	// basic cache for email connections
	static ConcurrentMap<String, Store> stores;
    static ConcurrentMap<ApiKey, String> emailMap;

	public SmsBackupUpdater() {
		super();
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws RateLimitReachedException, Exception {

        updateConnectorData(updateInfo);
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        String email = guestService.getApiKeyAttribute(updateInfo.apiKey, "username");
        String password = guestService.getApiKeyAttribute(updateInfo.apiKey,"password");
        for (ObjectType type : updateInfo.objectTypes()){
            Date since = getStartDate(updateInfo, type);
            if (type.name().equals("call_log")){
                List<ChannelMapping> mappings = bodyTrackHelper.getChannelMappings(updateInfo.apiKey);
                boolean call_logChannelExists = false;
                boolean photoChannelExists = false;
                for (ChannelMapping mapping: mappings){
                    if (mapping.deviceName.equals("sms_backup") && mapping.channelName.equals("call_log"))
                        call_logChannelExists = true;
                    if (mapping.deviceName.equals("sms_backup") && mapping.channelName.equals("photo"))
                        photoChannelExists = true;
                }
                if (!call_logChannelExists){
                    ChannelMapping mapping = new ChannelMapping();
                    mapping.deviceName = "sms_backup";
                    mapping.channelName = "call_log";
                    mapping.timeType = ChannelMapping.TimeType.gmt;
                    mapping.channelType = ChannelMapping.ChannelType.timespan;
                    mapping.guestId = updateInfo.getGuestId();
                    mapping.apiKeyId = updateInfo.apiKey.getId();
                    mapping.objectTypeId = type.value();
                    bodyTrackHelper.persistChannelMapping(mapping);

                    ChannelStyle channelStyle = new ChannelStyle();
                    channelStyle.timespanStyles = new MainTimespanStyle();
                    channelStyle.timespanStyles.defaultStyle = new TimespanStyle();
                    channelStyle.timespanStyles.defaultStyle.fillColor = "green";
                    channelStyle.timespanStyles.defaultStyle.borderColor = "#006000";
                    channelStyle.timespanStyles.defaultStyle.borderWidth = 2;
                    channelStyle.timespanStyles.defaultStyle.top = 0.0;
                    channelStyle.timespanStyles.defaultStyle.bottom = 1.0;

                    bodyTrackHelper.setBuiltinDefaultStyle(updateInfo.getGuestId(),"sms_backup","call_log",channelStyle);

                }
                if (!photoChannelExists){
                    ChannelMapping mapping;
                    mapping = new ChannelMapping();
                    mapping.deviceName = "sms_backup";
                    mapping.channelName = "photo";
                    mapping.timeType = ChannelMapping.TimeType.gmt;
                    mapping.channelType = ChannelMapping.ChannelType.photo;
                    mapping.guestId = updateInfo.getGuestId();
                    mapping.apiKeyId = updateInfo.apiKey.getId();
                    mapping.objectTypeId = ObjectType.getObjectType(updateInfo.apiKey.getConnector(),"sms").value();
                    bodyTrackHelper.persistChannelMapping(mapping);
                }
                retrieveCallLogSinceDate(updateInfo, email, password, since);
            }
            else if (type.name().equals("sms")){
                retrieveSmsEntriesSince(updateInfo, email, password, since);

            }
        }
	}


    public Date getStartDate(UpdateInfo updateInfo, ObjectType ot) {
        ApiKey apiKey = updateInfo.apiKey;

        String updateKeyName = "SMSBackup." + ot.getName() + ".updateStartDate";
        String updateStartDate = guestService.getApiKeyAttribute(apiKey, updateKeyName);

        if(updateStartDate == null) {
            updateStartDate = "0";

            guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, updateStartDate);
        }
        return new Date(Long.parseLong(updateStartDate));
    }

    private void updateStartDate(UpdateInfo updateInfo, ObjectType ot, long updateProgressTime){
        updateProgressTime -= 1; //incase we didn't pull 2 facets that occured at the same exact time

        // Calculate the name of the key in the ApiAttributes table
        // where the next start of update for this object type is
        // stored and retrieve the stored value.  This stored value
        // may potentially be null if something happened to the attributes table
        String updateKeyName = "SMSBackup." + ot.getName() + ".updateStartDate";
        long lastUpdateStart = getStartDate(updateInfo,ot).getTime();

        if (updateProgressTime <= lastUpdateStart) return;


        guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, "" + updateProgressTime);
    }

    private void updateStartDate(UpdateInfo updateInfo, ObjectType ot, Date updateProgressTime){
        updateStartDate(updateInfo, ot, updateProgressTime.getTime());
    }


	private AbstractFacet flushEntry(final UpdateInfo updateInfo, final String username, final Message message, Class type) throws Exception{
        final String messageId;
        final String smsBackupId;
        final String smsBackupAddress;
        if (message.getHeader("Message-ID") != null){
            messageId = message.getHeader("Message-ID")[0];
        }
        else if (message.getHeader("X-smssync-date") != null){
            messageId = message.getHeader("X-smssync-date")[0];
        }
        else{
            messageId = message.getHeader("X-backup2gmail-sms-date")[0];
        }
        if (message.getHeader("X-smssync-id") != null){
            smsBackupId = message.getHeader("X-smssync-id")[0];
        }
        else{
            smsBackupId = message.getHeader("X-backup2gmail-sms-id")[0];
        }
        if (message.getHeader("X-smssync-address") != null){
            smsBackupAddress = message.getHeader("X-smssync-address")[0];
        }
        else{
            smsBackupAddress = message.getHeader("X-backup2gmail-sms-address")[0];
        }
        final String emailId = messageId + smsBackupId;
        if (type == SmsEntryFacet.class){
            return apiDataService.createOrReadModifyWrite(SmsEntryFacet.class,
                                                   new FacetQuery(
                                                           "e.apiKeyId = ? AND e.emailId = ?",
                                                           updateInfo.apiKey.getId(),
                                                           emailId),
                                                   new FacetModifier<SmsEntryFacet>() {
                                                       // Throw exception if it turns out we can't make sense of the observation's JSON
                                                       // This will abort the transaction
                                                       @Override
                                                       public SmsEntryFacet createOrModify(SmsEntryFacet facet, Long apiKeyId) {
                                                           if (facet == null) {
                                                               facet = new SmsEntryFacet(updateInfo.apiKey.getId());
                                                               facet.emailId = emailId;
                                                               facet.guestId = updateInfo.apiKey.getGuestId();
                                                               facet.api = updateInfo.apiKey.getConnector().value();
                                                           }

                                                           facet.timeUpdated = System.currentTimeMillis();

                                                           try{
                                                               InternetAddress[] senders = (InternetAddress[]) message.getFrom();
                                                               InternetAddress[] recipients = (InternetAddress[]) message.getRecipients(Message.RecipientType.TO);
                                                               String fromAddress, toAddress;
                                                               boolean senderMissing = false, recipientsMissing = false;
                                                               if (senders != null && senders.length > 0){
                                                                   fromAddress = senders[0].getAddress();
                                                               }
                                                               else{
                                                                   fromAddress = message.getSubject().substring(9);
                                                                   senderMissing = true;
                                                               }
                                                               if (recipients != null && recipients.length > 0){
                                                                   toAddress =  recipients[0].getAddress();
                                                               }
                                                               else{
                                                                   toAddress = message.getSubject().substring(9);
                                                                   recipientsMissing = true;
                                                               }
                                                               if (fromAddress.startsWith(username)) {
                                                                   facet.smsType = SmsEntryFacet.SmsType.OUTGOING;
                                                                   if (recipientsMissing){
                                                                       facet.personName = toAddress;
                                                                       facet.personNumber = smsBackupAddress;
                                                                   }
                                                                   else if (toAddress.indexOf("unknown.email")!=-1) {
                                                                       facet.personName = recipients[0].getPersonal();
                                                                       facet.personNumber = toAddress.substring(0, toAddress.indexOf("@"));
                                                                   }
                                                                   else {
                                                                       facet.personName = recipients[0].getPersonal();
                                                                       facet.personNumber = smsBackupAddress;
                                                                   }
                                                               }else {
                                                                   facet.smsType = SmsEntryFacet.SmsType.INCOMING;
                                                                   if (senderMissing){
                                                                       facet.personName = fromAddress;
                                                                       facet.personNumber = smsBackupAddress;
                                                                   }
                                                                   else if (fromAddress.indexOf("unknown.email")!=-1) {
                                                                       facet.personName = senders[0].getPersonal();
                                                                       facet.personNumber = fromAddress.substring(0, fromAddress.indexOf("@"));
                                                                   }
                                                                   else {
                                                                       facet.personName = senders[0].getPersonal();
                                                                       facet.personNumber = smsBackupAddress;
                                                                   }
                                                               }
                                                               facet.dateReceived = message.getReceivedDate();
                                                               facet.start = facet.dateReceived.getTime();
                                                               facet.end = facet.start;
                                                               Object content = message.getContent();
                                                               facet.hasAttachments = false;
                                                               if (content instanceof String)
                                                                   facet.message = (String) message.getContent();
                                                               else if (content instanceof MimeMultipart) {//TODO: this is an MMS and needs to be handled properly
                                                                   facet.message = "";
                                                                   MimeMultipart multipart = (MimeMultipart) content;
                                                                   int partCount = multipart.getCount();
                                                                   for (int i = 0; i < partCount; i++){
                                                                       MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
                                                                       String contentType = part.getContentType().split(";")[0].toLowerCase();
                                                                       Object partContent = part.getContent();
                                                                       if (contentType.startsWith("text/plain")){//other types of text are returned as byte streams and are attachments
                                                                            if (!facet.message.equals("")){
                                                                                facet.message += "\n\n";
                                                                            }
                                                                           facet.message = (String) partContent;
                                                                       }
                                                                       else{
                                                                           if (!facet.hasAttachments){
                                                                               facet.hasAttachments = true;
                                                                               facet.attachmentMimeTypes = contentType;
                                                                               facet.attachmentNames = (emailId + i).replaceAll("\\W+","");
                                                                           }
                                                                           else{
                                                                               facet.attachmentMimeTypes += "," + contentType;
                                                                               facet.attachmentNames += "," + (emailId + i).replaceAll("\\W+","");

                                                                           }

                                                                           File attachmentFile = getAttachmentFile(env.targetEnvironmentProps.getString("btdatastore.db.location"),updateInfo.getGuestId(),updateInfo.apiKey.getId(),(emailId + i).replaceAll("\\W+",""));
                                                                           attachmentFile.getParentFile().mkdirs();
                                                                           FileOutputStream fileoutput = new FileOutputStream(attachmentFile);
                                                                           IOUtils.copy((BASE64DecoderStream) partContent, fileoutput);
                                                                           fileoutput.close();
                                                                       }
                                                                   }
                                                               }
                                                           }  catch(Exception e){
                                                               e.printStackTrace();
                                                               return null;
                                                           }
                                                           return facet;
                                                       }
                                                   }, updateInfo.apiKey.getId());

        }
        else if (type == CallLogEntryFacet.class){
            return apiDataService.createOrReadModifyWrite(CallLogEntryFacet.class,
                                                   new FacetQuery(
                                                           "e.apiKeyId = ? AND e.emailId = ?",
                                                           updateInfo.apiKey.getId(),
                                                           emailId),
                                                   new FacetModifier<CallLogEntryFacet>() {
                                                       // Throw exception if it turns out we can't make sense of the observation's JSON
                                                       // This will abort the transaction
                                                       @Override
                                                       public CallLogEntryFacet createOrModify(CallLogEntryFacet facet, Long apiKeyId) {
                                                           if (facet == null) {
                                                               facet = new CallLogEntryFacet(updateInfo.apiKey.getId());
                                                               facet.emailId = emailId;
                                                               facet.guestId = updateInfo.apiKey.getGuestId();
                                                               facet.api = updateInfo.apiKey.getConnector().value();
                                                           }

                                                           facet.timeUpdated = System.currentTimeMillis();

                                                           try{
                                                               List<String> lines = IOUtils.readLines(new StringReader((String)message.getContent()));
                                                               if (lines.size()==2) {
                                                                   String timeLine = lines.get(0);
                                                                   String callLine = lines.get(1);
                                                                   StringTokenizer st = new StringTokenizer(timeLine);
                                                                   String secsString = st.nextToken();
                                                                   facet.seconds = Integer.parseInt(secsString.substring(0,secsString.length()-1));
                                                                   st = new StringTokenizer(callLine);
                                                                   if (callLine.indexOf("outgoing call")!=-1) {
                                                                       facet.callType = CallLogEntryFacet.CallType.OUTGOING;
                                                                   } else if (callLine.indexOf("incoming call")!=-1) {
                                                                       facet.callType = CallLogEntryFacet.CallType.INCOMING;
                                                                   }
                                                                   facet.personNumber = st.nextToken();
                                                                   switch(facet.callType) {
                                                                       case OUTGOING:
                                                                           Address[] recipients = message.getRecipients(Message.RecipientType.TO);
                                                                           if (recipients != null && recipients.length > 0)
                                                                               facet.personName = ((InternetAddress)recipients[0]).getPersonal();
                                                                           else
                                                                               facet.personName = message.getSubject().substring(10);//read the name from the subject line
                                                                           break;
                                                                       case INCOMING:
                                                                           Address[] senders = message.getFrom();
                                                                           if (senders != null && senders.length > 0)
                                                                               facet.personName = ((InternetAddress)senders[0]).getPersonal();
                                                                           else
                                                                               facet.personName = message.getSubject().substring(10);//read the name from the subject line
                                                                   }
                                                               } else if (lines.size()==1) {
                                                                   String callLine = lines.get(0);
                                                                   StringTokenizer st = new StringTokenizer(callLine);
                                                                   facet.personNumber = st.nextToken();
                                                                   facet.callType = CallLogEntryFacet.CallType.MISSED;
                                                                   Address[] senders = message.getFrom();
                                                                   if (senders != null && senders.length > 0)
                                                                       facet.personName = ((InternetAddress)senders[0]).getPersonal();
                                                                   else
                                                                       facet.personName = message.getSubject().substring(10);//read the name from the subject line
                                                               }
                                                               facet.date = message.getReceivedDate();
                                                               facet.start = facet.date.getTime();
                                                               facet.end = facet.start + facet.seconds*1000;
                                                           }
                                                           catch (Exception e){
                                                               e.printStackTrace();
                                                               return null;
                                                           }

                                                           return facet;
                                                       }
                                                   }, updateInfo.apiKey.getId());

        }
        else{
            return null;
        }
	}

    public static File getAttachmentFile(String kvsLocation, long guestId, long apiKeyId, String attachmentName){

        return new File(kvsLocation + File.separator + guestId + File.separator
                             + Connector.getConnector("sms_backup").prettyName() + File.separator
                             + apiKeyId + File.separator + attachmentName);

    }

    private GoogleCredential getCredentials(ApiKey apiKey) throws UpdateFailedException{
        HttpTransport httpTransport = new NetHttpTransport();
        JacksonFactory jsonFactory = new JacksonFactory();
        // Get all the attributes for this connector's oauth token from the stored attributes
        String accessToken = guestService.getApiKeyAttribute(apiKey, "accessToken");
        final String refreshToken = guestService.getApiKeyAttribute(apiKey, "refreshToken");
        final String clientId = guestService.getApiKeyAttribute(apiKey, "google.client.id");
        final String clientSecret = guestService.getApiKeyAttribute(apiKey,"google.client.secret");
        final GoogleCredential.Builder builder = new GoogleCredential.Builder();
        builder.setTransport(httpTransport);
        builder.setJsonFactory(jsonFactory);
        builder.setClientSecrets(clientId, clientSecret);
        GoogleCredential credential = builder.build();
        final Long tokenExpires = Long.valueOf(guestService.getApiKeyAttribute(apiKey, "tokenExpires"));
        credential.setExpirationTimeMilliseconds(tokenExpires);
        credential.setAccessToken(accessToken);
        credential.setRefreshToken(refreshToken);

        try {
            if (tokenExpires<System.currentTimeMillis()) {
                boolean tokenRefreshed = false;

                // Don't worry about checking if we are running on a mirrored test instance.
                // Refreshing tokens independently on both the main server and a mirrored instance
                // seems to work just fine.

                // Try to swap the expired access token for a fresh one.
                tokenRefreshed = credential.refreshToken();

                if(tokenRefreshed) {
                    Long newExpireTime = credential.getExpirationTimeMilliseconds();
                    // Update stored expire time
                    guestService.setApiKeyAttribute(apiKey, "accessToken", credential.getAccessToken());
                    guestService.setApiKeyAttribute(apiKey, "tokenExpires", newExpireTime.toString());
                }
            }
        }
        catch (TokenResponseException e) {
            // Notify the user that the tokens need to be manually renewed
            notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.WARNING, connector().statusNotificationName(),
                                                      "Heads Up. We failed in our attempt to automatically refresh your Google authentication tokens.<br>" +
                                                      "Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                      "scroll to the Google Calendar connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");

            // Record permanent update failure since this connector is never
            // going to succeed
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, Utils.stackTrace(e));
            throw new UpdateFailedException("refresh token attempt permanently failed due to a bad token refresh response", e, true);
        }
        catch (IOException e) {
            // Notify the user that the tokens need to be manually renewed
            throw new UpdateFailedException("refresh token attempt failed", e, true);
        }

        return credential;
    }

    private String getEmailAddress(ApiKey apiKey) throws UpdateFailedException{

        if (emailMap == null){
            emailMap = new ConcurrentLinkedHashMap.Builder<ApiKey, String>()
                    .maximumWeightedCapacity(100).build();
        }

        if (emailMap.containsKey(apiKey)){
            return emailMap.get(apiKey);
        }

        HttpTransport httpTransport = new NetHttpTransport();
        JacksonFactory jsonFactory = new JacksonFactory();
        GoogleCredential credential = getCredentials(apiKey);
        String emailAddress = null;

        try{
            Plus plus = new Plus(httpTransport, jsonFactory, credential);
            Person mePerson = plus.people().get("me").execute();
            List<Person.Emails> emails = mePerson.getEmails();
            for (Person.Emails email : emails){
                if (email.getType().equals("account")){
                    emailAddress = email.getValue();
                }
            }
            if (emailAddress == null)
                throw new Exception("Account email not in email list");
            emailMap.put(apiKey,emailAddress);
            return emailAddress;
        }
        catch (Exception e){
            throw new UpdateFailedException("Failed to get gmail address!",e,false);
        }

    }

    private Store getStore(ApiKey apiKey) throws UpdateFailedException{
        String emailAddress = getEmailAddress(apiKey);
        GoogleCredential credential = getCredentials(apiKey);




        String accessToken = credential.getAccessToken();



        try{
            Store store = MailUtils.getGmailImapStoreViaSASL(emailAddress, accessToken);
            return store;
        } catch(Exception e){
            throw new UpdateFailedException("Failed to connect to gmail!",e,false);
        }


    }

	private Store getStore(String email, String password)
			throws MessagingException {
		if (stores == null)
			stores = new ConcurrentLinkedHashMap.Builder<String, Store>()
					.maximumWeightedCapacity(100).build();
		Store store = null;
		if (stores.get(email + "-basicAuth") != null) {
			store = stores.get(email + "-basicAuth");
			if (!store.isConnected())
				store.connect();
			boolean stillAlive = true;
			try {
				store.getDefaultFolder();
			} catch (Exception e) {
				stillAlive = false;
			}
			if (stillAlive)
				return store;
            else
                store.close();
		}
		store = MailUtils.getGmailImapStore(email, password);
		stores.put(email + "-basicAuth", store);
		return store;
	}

	void retrieveSmsEntriesSince(UpdateInfo updateInfo,
			String email, String password, Date date) throws Exception {
		long then = System.currentTimeMillis();
		String query = "(incremental sms log retrieval)";
		ObjectType smsObjectType = ObjectType.getObjectType(connector(), "sms");
		String smsFolderName = getSettingsOrPortLegacySettings(updateInfo.apiKey).smsFolderName;
		try {
            Store store;
            if (guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken") != null){
			    store = getStore(updateInfo.apiKey);
                email = getEmailAddress(updateInfo.apiKey);
            }
            else
                store = getStore(email,password);
			Folder folder = store.getDefaultFolder();
			if (folder == null  || !folder.exists())
				throw new FolderNotFoundException();
			folder = folder.getFolder(smsFolderName);
			if (folder == null  || !folder.exists())
				throw new FolderNotFoundException();
			Message[] msgs = getMessagesInFolderSinceDate(folder, date);

            //if we get to this point then we were able to access the folder and should delete our error notification
            Notification errorNotification = notificationsService.getNamedNotification(updateInfo.getGuestId(), connector().getName() + ".smsFolderError");
            if (errorNotification != null && !errorNotification.deleted){
                notificationsService.deleteNotification(updateInfo.getGuestId(),errorNotification.getId());
            }

			for (Message message : msgs) {
                date = message.getReceivedDate();
                if (flushEntry(updateInfo, email, message, SmsEntryFacet.class) == null){
                    throw new Exception("Could not persist SMS message");
                }
                updateStartDate(updateInfo, smsObjectType, date);
			}
			countSuccessfulApiCall(updateInfo.apiKey,
					smsObjectType.value(), then, query);
			return;
		} catch (MessagingException ex){
            notificationsService.addNamedNotification(updateInfo.getGuestId(),
                                                      Notification.Type.ERROR, connector().getName() + ".smsFolderError",
                                                      "The SMS folder configured for SMS Backup, \"" + smsFolderName + "\", does not exist. Either change it in your connector settings or check if SMS Backup is set to use this folder.");
            throw new UpdateFailedException("Couldn't open SMS folder.",false);
        }
        catch (Exception ex) {
            ex.printStackTrace();
			reportFailedApiCall(updateInfo.apiKey, smsObjectType.value(),
					then, query, Utils.stackTrace(ex), ex.getMessage());
			throw ex;
		}
	}

	void retrieveCallLogSinceDate(UpdateInfo updateInfo,
			String email, String password, Date date) throws Exception {
        //if (true)
        //    throw new Exception("Blah");
		long then = System.currentTimeMillis();
		String query = "(incremental call log retrieval)";
		ObjectType callLogObjectType = ObjectType.getObjectType(connector(),
				"call_log");
		String callLogFolderName = getSettingsOrPortLegacySettings(updateInfo.apiKey).callLogFolderName;
		try {
            Store store;
            if (guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken") != null){
                store = getStore(updateInfo.apiKey);
                email = getEmailAddress(updateInfo.apiKey);
            }
            else
                store = getStore(email,password);
			Folder folder = store.getDefaultFolder();
			if (folder == null || !folder.exists())
				throw new FolderNotFoundException();
			folder = folder.getFolder(callLogFolderName);
			if (folder == null || !folder.exists())
				throw new FolderNotFoundException();
			Message[] msgs = getMessagesInFolderSinceDate(folder, date);

            //if we get to this point then we were able to access the folder and should delete our error notification
            Notification errorNotification = notificationsService.getNamedNotification(updateInfo.getGuestId(), connector().getName() + ".callLogFolderError");
            if (errorNotification != null && !errorNotification.deleted){
                notificationsService.deleteNotification(updateInfo.getGuestId(),errorNotification.getId());
            }

			for (Message message : msgs) {
                date = message.getReceivedDate();
                if (flushEntry(updateInfo, email, message, CallLogEntryFacet.class) == null){
                    throw new Exception("Could not persist Call log");
                }
                updateStartDate(updateInfo,callLogObjectType,date);
			}
			countSuccessfulApiCall(updateInfo.apiKey,
					callLogObjectType.value(), then, query);
			return;
		}
        catch (MessagingException ex){
            notificationsService.addNamedNotification(updateInfo.getGuestId(), Notification.Type.ERROR, connector().getName() + ".callLogFolderError",
                                  "The call log folder configured for SMS Backup, \"" + callLogFolderName + "\", does not exist. Either change it in your connector settings or check if SMS Backup is set to use this folder.");
            throw new UpdateFailedException("Couldn't open Call Log folder.",false);
        }
        catch (Exception ex) {
            ex.printStackTrace();
			reportFailedApiCall(updateInfo.apiKey,
					callLogObjectType.value(), then, query, Utils.stackTrace(ex),
                    ex.getMessage());
			throw ex;
		}
	}

	private Message[] getMessagesInFolderSinceDate(Folder folder, Date date)
			throws Exception {
		if (!folder.isOpen())
			folder.open(Folder.READ_ONLY);
		SentDateTerm term = new SentDateTerm(SentDateTerm.GT, date);
		Message[] msgs = folder.search(term);
		return msgs;
	}

    private SmsBackupSettings getSettingsOrPortLegacySettings(final ApiKey apiKey){
        SmsBackupSettings settings = (SmsBackupSettings)apiKey.getSettings();
        boolean persistSettings = false;
        if (settings == null){
            settings = new SmsBackupSettings();
            persistSettings = true;
        }

        if (settings.smsFolderName == null){
            String oldSmsFolder = guestService.getApiKeyAttribute(apiKey,"smsFolderName");
            if (oldSmsFolder != null){
                settings.smsFolderName = oldSmsFolder;
                guestService.removeApiKeyAttribute(apiKey.getId(),"smsFolderName");
            }
            else{
                settings.smsFolderName = "";
            }
            persistSettings = true;
        }
        if (settings.callLogFolderName == null){
            String oldCallLogFolder = guestService.getApiKeyAttribute(apiKey,"callLogFolderName");
            if (oldCallLogFolder != null){
                settings.callLogFolderName = oldCallLogFolder;
                guestService.removeApiKeyAttribute(apiKey.getId(),"callLogFolderName");
            }
            else{
                settings.callLogFolderName = "";
            }
            persistSettings = true;
        }
        if (settings.smsFolderName.equals("")){
            settings.smsFolderName = "SMS";
            persistSettings = true;
        }
        if (settings.callLogFolderName.equals("")){
            settings.callLogFolderName = "Call log";
            persistSettings = true;
        }
        if (persistSettings){
            settingsService.saveConnectorSettings(apiKey.getId(),settings);
        }
        return settings;
    }

    @Override
    public void connectorSettingsChanged(final long apiKeyId, final Object settings) {
    }

    @Override
    public Object syncConnectorSettings(final UpdateInfo updateInfo, final Object settings) {
        return settings;
    }
}
