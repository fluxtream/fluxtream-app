package org.fluxtream.connectors.sms_backup;

import java.io.*;
import java.lang.Thread;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.search.SentDateTerm;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.model.Message;
import org.apache.commons.codec.binary.Base64;
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

    static final int baseSleepAmount = 500; //half a second
    static final int maxSleepAmount = 1000 * 60 * 60; //one hour

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
        for (ObjectType type : updateInfo.objectTypes()){
            BigInteger historyId = getHistoryId(updateInfo, type);
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
                retrieveCallLogSinceDate(updateInfo, historyId);
            }
            else if (type.name().equals("sms")){
                retrieveSmsEntriesSince(updateInfo, historyId);

            }
        }
	}


    public BigInteger getHistoryId(UpdateInfo updateInfo, ObjectType ot){
        ApiKey apiKey = updateInfo.apiKey;

        String updateKeyName = "SMSBackup." + ot.getName() + ".historyId";
        String historyIdString = guestService.getApiKeyAttribute(apiKey, updateKeyName);

        if (historyIdString == null)
            return null;
        return new BigInteger(historyIdString);
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

    private void updateHistoryId(UpdateInfo updateInfo, ObjectType ot, BigInteger historyId){
        BigInteger oldHistoryId = getHistoryId(updateInfo, ot);
        //in case we ran some other update at the same time, we shouldn't overwrite it
        if (oldHistoryId != null && oldHistoryId.compareTo(historyId) >= 0){
            return;
        }
        String updateKeyName = "SMSBackup." + ot.getName() + ".historyId";
        guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, historyId.toString());
    }

    private void updateStartDate(UpdateInfo updateInfo, ObjectType ot, Date updateProgressTime){
        updateStartDate(updateInfo, ot, updateProgressTime.getTime());
    }


	private AbstractFacet flushEntry(final UpdateInfo updateInfo, final String username, final MimeMessage message, Class type) throws Exception{
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
                                                               InternetAddress[] senders = null;
                                                               try{
                                                                   senders =  (InternetAddress[]) message.getFrom();
                                                               } catch (AddressException ignored){}
                                                               InternetAddress[] recipients = null;
                                                               try{
                                                                   recipients =  (InternetAddress[]) message.getRecipients(MimeMessage.RecipientType.TO);
                                                               } catch (AddressException ignored){}
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
                                                               facet.dateReceived = message.getSentDate();
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
                                                                           Address[] recipients = null;
                                                                           try{
                                                                               recipients =  message.getRecipients(MimeMessage.RecipientType.TO);
                                                                           } catch (AddressException ignored){}
                                                                           if (recipients != null && recipients.length > 0)
                                                                               facet.personName = ((InternetAddress)recipients[0]).getPersonal();
                                                                           else
                                                                               facet.personName = message.getSubject().substring(10);//read the name from the subject line
                                                                           break;
                                                                       case INCOMING:
                                                                           Address[] senders = null;
                                                                           try{
                                                                               senders =  message.getFrom();
                                                                           } catch (AddressException ignored){}
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
                                                                   Address[] senders = null;
                                                                   try{
                                                                       senders =  message.getFrom();
                                                                   } catch (AddressException ignored){}
                                                                   if (senders != null && senders.length > 0)
                                                                       facet.personName = ((InternetAddress)senders[0]).getPersonal();
                                                                   else
                                                                       facet.personName = message.getSubject().substring(10);//read the name from the subject line
                                                               }
                                                               facet.date = message.getSentDate();
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
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, Utils.stackTrace(e), ApiKey.PermanentFailReason.NEEDS_REAUTH);
            throw new UpdateFailedException("refresh token attempt permanently failed due to a bad token refresh response", e, true, ApiKey.PermanentFailReason.NEEDS_REAUTH);
        }
        catch (IOException e) {
            // Notify the user that the tokens need to be manually renewed
            throw new UpdateFailedException("refresh token attempt failed", e, true, ApiKey.PermanentFailReason.NEEDS_REAUTH);
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
            throw new UpdateFailedException("Failed to get gmail address!", e, false, null);
        }

    }

    private Gmail getGmailService(ApiKey apiKey) throws UpdateFailedException{
        HttpTransport httpTransport = new NetHttpTransport();
        JacksonFactory jsonFactory = new JacksonFactory();
        GoogleCredential credential = getCredentials(apiKey);
        return new Gmail(httpTransport, jsonFactory, credential);
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

	void retrieveSmsEntriesSince(UpdateInfo updateInfo, BigInteger historyId) throws Exception {
		long then = System.currentTimeMillis();
		String query = "(incremental sms log retrieval)";
		ObjectType smsObjectType = ObjectType.getObjectType(connector(), "sms");
		String smsFolderName = getSettingsOrPortLegacySettings(updateInfo.apiKey).smsFolderName;
		try {
            Gmail gmail = getGmailService(updateInfo.apiKey);
            String email = getEmailAddress(updateInfo.apiKey);

            BigInteger originalHistoryId = historyId;

            Label smsLabel = null;

            for (Label label : gmail.users().labels().list(email).execute().getLabels()){
                if (label.getName().equals(smsFolderName)){
                    smsLabel = label;
                }
            }
            if (smsLabel == null)
                throw new FolderNotFoundException();

            //if we get to this point then we were able to access the folder and should delete our error notification
            Notification errorNotification = notificationsService.getNamedNotification(updateInfo.getGuestId(), connector().getName() + ".smsFolderError");
            if (errorNotification != null && !errorNotification.deleted){
                notificationsService.deleteNotification(updateInfo.getGuestId(),errorNotification.getId());
            }

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            if (historyId != null){
                ListHistoryResponse historyResponse = null;
                BigInteger queryHistoryId = historyId;
                do{
                    historyResponse = invokeListHistory(gmail,email,queryHistoryId,smsLabel.getId(),historyResponse == null ? null : historyResponse.getNextPageToken());
                    if (historyResponse == null){
                        //if historyResponse is null that means we got a 404 error which means historyId is no longer valid
                        historyId = null;
                        break;
                    }
                    List<History> histories = historyResponse.getHistory();
                    for (History history : histories){
                        if (history.getMessages() == null)
                            continue;
                        for (Message messageStub : history.getMessages()){
                            Message message = invokeGetMessage(gmail, email, messageStub.getId());
                            if (message == null)
                                continue;
                            if (historyId.compareTo(message.getHistoryId()) < 0)
                                historyId = message.getHistoryId();
                            byte[] emailBytes = Base64.decodeBase64(message.getRaw());
                            MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(emailBytes));

                            if (flushEntry(updateInfo, email, mimeMessage, SmsEntryFacet.class) == null){
                                throw new Exception("Could not persist SMS");
                            }
                        }
                    }
                } while (historyResponse.getNextPageToken() != null);
            }
            if (historyId == null){
                ListMessagesResponse listResponse = null;
                do{
                    listResponse = invokeList(gmail,email,smsLabel.getId(),listResponse == null ? null : listResponse.getNextPageToken());
                    if (listResponse.getMessages() == null){
                        continue;
                    }
                    for (Message messageStub : listResponse.getMessages()){
                        Message message = invokeGetMessage(gmail, email, messageStub.getId());
                        if (message == null)
                            continue;
                        if (historyId == null || historyId.compareTo(message.getHistoryId()) < 0)
                            historyId = message.getHistoryId();
                        byte[] emailBytes = Base64.decodeBase64(message.getRaw());
                        MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(emailBytes));

                        if (flushEntry(updateInfo, email, mimeMessage, SmsEntryFacet.class) == null){
                            throw new Exception("Could not persist SMS");
                        }
                    }

                } while (listResponse.getNextPageToken() != null);
            }
            if (historyId != null && !historyId.equals(originalHistoryId)){
                updateHistoryId(updateInfo,smsObjectType,historyId);
            }

		} catch (MessagingException ex){
            notificationsService.addNamedNotification(updateInfo.getGuestId(),
                                                      Notification.Type.ERROR, connector().getName() + ".smsFolderError",
                                                      "The SMS folder configured for SMS Backup, \"" + smsFolderName + "\", does not exist. Either change it in your connector settings or check if SMS Backup is set to use this folder.");
            throw new UpdateFailedException("Couldn't open SMS folder.",false, null);
        }
        catch (Exception ex) {
            ex.printStackTrace();
			reportFailedApiCall(updateInfo.apiKey, smsObjectType.value(),
					then, query, Utils.stackTrace(ex), ex.getMessage());
			throw ex;
		}
	}

	void retrieveCallLogSinceDate(UpdateInfo updateInfo, BigInteger historyId) throws Exception {
		long then = System.currentTimeMillis();
		String query = "(incremental call log retrieval)";
		ObjectType callLogObjectType = ObjectType.getObjectType(connector(),
				"call_log");
		String callLogFolderName = getSettingsOrPortLegacySettings(updateInfo.apiKey).callLogFolderName;
		try {
            Gmail gmail = getGmailService(updateInfo.apiKey);
            String email = getEmailAddress(updateInfo.apiKey);

            BigInteger originalHistoryId = historyId;

            Label callLogLabel = null;

            for (Label label : gmail.users().labels().list(email).execute().getLabels()){
                if (label.getName().equals(callLogFolderName)){
                    callLogLabel = label;
                }
            }
            if (callLogLabel == null)
                throw new FolderNotFoundException();

            //if we get to this point then we were able to access the folder and should delete our error notification
            Notification errorNotification = notificationsService.getNamedNotification(updateInfo.getGuestId(), connector().getName() + ".callLogFolderError");
            if (errorNotification != null && !errorNotification.deleted){
                notificationsService.deleteNotification(updateInfo.getGuestId(),errorNotification.getId());
            }

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            if (historyId != null){
                ListHistoryResponse historyResponse = null;
                BigInteger queryHistoryId = historyId;
                do{
                    historyResponse = invokeListHistory(gmail,email,queryHistoryId,callLogLabel.getId(),historyResponse == null ? null : historyResponse.getNextPageToken());
                    if (historyResponse == null){
                        //if historyResponse is null that means we got a 404 error which means historyId is no longer valid
                        historyId = null;
                        break;
                    }
                    List<History> histories = historyResponse.getHistory();
                    for (History history : histories){
                        if (history.getMessages() == null)
                            continue;
                        for (Message messageStub : history.getMessages()){
                            Message message = invokeGetMessage(gmail, email, messageStub.getId());
                            if (message == null)
                                continue;
                            if (historyId.compareTo(message.getHistoryId()) < 0)
                                historyId = message.getHistoryId();
                            byte[] emailBytes = Base64.decodeBase64(message.getRaw());
                            MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(emailBytes));

                            if (flushEntry(updateInfo, email, mimeMessage, CallLogEntryFacet.class) == null){
                                throw new Exception("Could not persist Call log");
                            }
                        }
                    }
                } while (historyResponse.getNextPageToken() != null);
            }
            if (historyId == null){
                ListMessagesResponse listResponse = null;
                do{
                    listResponse = invokeList(gmail,email,callLogLabel.getId(),listResponse == null ? null : listResponse.getNextPageToken());
                    if (listResponse.getMessages() == null){
                        continue;
                    }
                    for (Message messageStub : listResponse.getMessages()){
                        Message message = invokeGetMessage(gmail, email, messageStub.getId());
                        if (message == null)
                            continue;
                        if (historyId == null || historyId.compareTo(message.getHistoryId()) < 0)
                            historyId = message.getHistoryId();
                        byte[] emailBytes = Base64.decodeBase64(message.getRaw());
                        MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(emailBytes));

                        if (flushEntry(updateInfo, email, mimeMessage, CallLogEntryFacet.class) == null){
                            throw new Exception("Could not persist Call log");
                        }
                    }

                } while (listResponse.getNextPageToken() != null);
            }
            if (historyId != null && !historyId.equals(originalHistoryId)){
                updateHistoryId(updateInfo,callLogObjectType,historyId);
            }

			countSuccessfulApiCall(updateInfo.apiKey, callLogObjectType.value(), then, query);
		}
        catch (MessagingException ex){
            notificationsService.addNamedNotification(updateInfo.getGuestId(), Notification.Type.ERROR, connector().getName() + ".callLogFolderError",
                                  "The call log folder configured for SMS Backup, \"" + callLogFolderName + "\", does not exist. Either change it in your connector settings or check if SMS Backup is set to use this folder.");
            throw new UpdateFailedException("Couldn't open Call Log folder.",false, null);
        }
        catch (Exception ex) {
            ex.printStackTrace();
			reportFailedApiCall(updateInfo.apiKey,
					callLogObjectType.value(), then, query, Utils.stackTrace(ex),
                    ex.getMessage());
			throw ex;
		}
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

    private Message invokeGetMessage(Gmail gmail, String email, String messageId) throws IOException {
        Gmail.Users.Messages.Get messageQuery = gmail.users().messages().get(email, messageId).setFormat("raw");

        Message message = null;

        int sleepTime = baseSleepAmount;

        while (message == null){
            try{
                message = messageQuery.execute();
            }
            catch (java.net.SocketTimeoutException ex){
                try{
                    sleepTime = Math.min(sleepTime * 2, maxSleepAmount);
                    Thread.sleep(sleepTime);
                } catch (Exception ignored){}
            }
            catch (GoogleJsonResponseException responseException){
                switch (responseException.getDetails().getCode()){
                    case 500://internal server error, should resolve
                    case 503://internal server error, should resolve
                    case 429://per second rate limit, just need to sleep
                        try{
                            System.err.println("SmsBackUpdater.invokeGetMessage: Error " + responseException.getDetails().getCode());
                            sleepTime = Math.min(sleepTime * 2, maxSleepAmount);
                            Thread.sleep(sleepTime);
                        } catch (Exception ignored){}
                        break;
                    case 404://not found/invalid message id (could happen if the message was deleted before we queried for it)
                        return null;
                    case 401://Unauthorized (should indicate that our auth info is invalid)
                    default:
                        throw responseException;

                }
            }
        }

        return message;
    }

    private ListMessagesResponse invokeList(Gmail gmail, String email, List<String> labels, String nextPageToken) throws IOException {
        Gmail.Users.Messages.List messagesQuery = gmail.users().messages().list(email).setLabelIds(labels);
        if (nextPageToken != null){
            messagesQuery.setPageToken(nextPageToken);
        }
        ListMessagesResponse response = null;

        int sleepTime = baseSleepAmount;

        while (response == null){
            try{
                response = messagesQuery.execute();
            }
            catch (java.net.SocketTimeoutException ex){
                try{
                    sleepTime = Math.min(sleepTime * 2, maxSleepAmount);
                    Thread.sleep(sleepTime);
                } catch (Exception ignored){}
            }
            catch (GoogleJsonResponseException responseException){
                switch (responseException.getDetails().getCode()){
                    case 500://internal server error, should resolve
                    case 503://internal server error, should resolve
                    case 429://per second rate limit, just need to sleep
                        try{
                            System.err.println("SmsBackUpdater.invokeList: Error " + responseException.getDetails().getCode());
                            sleepTime = Math.min(sleepTime * 2, maxSleepAmount);
                            Thread.sleep(sleepTime);
                        } catch (Exception ignored){}
                        break;
                    case 401://Unauthorized
                    default:
                        throw responseException;

                }
            }

        }
        return response;
    }

    private ListMessagesResponse invokeList(Gmail gmail, String email, String labelId, String nextPageToken) throws IOException {
        List<String> list = new ArrayList<String>();
        list.add(labelId);
        return invokeList(gmail,email,list,nextPageToken);
    }

    private ListHistoryResponse invokeListHistory(Gmail gmail, String email, BigInteger historyId, String labelId, String nextPageToken) throws IOException {
        Gmail.Users.History.List historyQuery = gmail.users().history().list(email).setStartHistoryId(historyId).setLabelId(labelId);
        if (nextPageToken != null){
            historyQuery.setPageToken(nextPageToken);
        }

        ListHistoryResponse response = null;
        int sleepTime = baseSleepAmount;

        while (response == null){
            try{
                response = historyQuery.execute();
            }
            catch (java.net.SocketTimeoutException ex){
                try{
                    sleepTime = Math.min(sleepTime * 2, maxSleepAmount);
                    Thread.sleep(sleepTime);
                } catch (Exception ignored){}
            }
            catch (GoogleJsonResponseException responseException){
                switch (responseException.getDetails().getCode()){
                    case 500://internal server error, should resolve
                    case 503://internal server error, should resolve
                    case 429://per second rate limit, just need to sleep
                        try{
                            System.err.println("SmsBackUpdater.invokeListHistory: Error " + responseException.getDetails().getCode());
                            sleepTime = Math.min(sleepTime * 2, maxSleepAmount);
                            Thread.sleep(sleepTime);
                        } catch (Exception ignored){}
                        break;
                    case 404://not found/invalid historyid
                        return null;
                    case 401://Unauthorized
                    default:
                        throw responseException;

                }
            }

        }
        return response;

    }
}
