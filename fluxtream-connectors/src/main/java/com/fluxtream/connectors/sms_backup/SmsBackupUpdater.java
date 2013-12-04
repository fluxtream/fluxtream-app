package com.fluxtream.connectors.sms_backup;

import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.SentDateTerm;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ChannelMapping;
import com.fluxtream.services.ApiDataService.FacetModifier;
import com.fluxtream.services.ApiDataService.FacetQuery;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.fluxtream.services.impl.BodyTrackHelper.ChannelStyle;
import com.fluxtream.services.impl.BodyTrackHelper.MainTimespanStyle;
import com.fluxtream.services.impl.BodyTrackHelper.TimespanStyle;
import com.fluxtream.utils.MailUtils;
import com.fluxtream.utils.Utils;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.ibm.icu.util.StringTokenizer;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "SMS Backup", value = 6, objectTypes = {
		CallLogEntryFacet.class, SmsEntryFacet.class },
         defaultChannels = {"sms_backup.call_log"})
public class SmsBackupUpdater extends AbstractUpdater {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

	// basic cache for email connections
	static ConcurrentMap<String, Store> stores;

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
                List<ChannelMapping> mappings = bodyTrackHelper.getChannelMappings(updateInfo.apiKey, type.value());
                if (mappings.size() == 0){
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
        updateStartDate(updateInfo,ot,updateProgressTime.getTime());
    }


	private void flushEntry(final UpdateInfo updateInfo, final String username, final Message message, Class type) throws Exception {
        final String emailId = message.getHeader("Message-ID")[0] + message.getHeader("X-smssync-id")[0];
        if (type == SmsEntryFacet.class){
            apiDataService.createOrReadModifyWrite(SmsEntryFacet.class,
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
                                                                       facet.personNumber = message.getHeader("X-smssync-address")[0];
                                                                   }
                                                                   else if (toAddress.indexOf("unknown.email")!=-1) {
                                                                       facet.personName = recipients[0].getPersonal();
                                                                       facet.personNumber = toAddress.substring(0, toAddress.indexOf("@"));
                                                                   }
                                                                   else {
                                                                       facet.personName = recipients[0].getPersonal();
                                                                       facet.personNumber = message.getHeader("X-smssync-address")[0];
                                                                   }
                                                               }else {
                                                                   facet.smsType = SmsEntryFacet.SmsType.INCOMING;
                                                                   if (senderMissing){
                                                                       facet.personName = fromAddress;
                                                                       facet.personNumber = message.getHeader("X-smssync-address")[0];
                                                                   }
                                                                   else if (fromAddress.indexOf("unknown.email")!=-1) {
                                                                       facet.personName = senders[0].getPersonal();
                                                                       facet.personNumber = fromAddress.substring(0, fromAddress.indexOf("@"));
                                                                   }
                                                                   else {
                                                                       facet.personName = senders[0].getPersonal();
                                                                       facet.personNumber = message.getHeader("X-smssync-address")[0];
                                                                   }
                                                               }
                                                               facet.dateReceived = message.getReceivedDate();
                                                               facet.start = facet.dateReceived.getTime();
                                                               facet.end = facet.start;
                                                               Object content = message.getContent();
                                                               if (content instanceof String)
                                                                   facet.message = (String) message.getContent();
                                                               else if (content instanceof MimeMultipart) {//TODO: this is an MMS and needs to be handled properly
                                                                   String contentType = ((MimeMultipart) content).getContentType();
                                                                   facet.message = "message of type " + contentType;
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
            apiDataService.createOrReadModifyWrite(CallLogEntryFacet.class,
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
	}

	static boolean checkAuthorization(GuestService guestService, Long guestId) {
		ApiKey apiKey = guestService.getApiKey(guestId,
				Connector.getConnector("sms_backup"));
		return apiKey != null;
	}

	boolean testConnection(String email, String password) {
		try {
			MailUtils.getGmailImapStore(email, password);
		} catch (MessagingException e) {
			return false;
		}
		return true;
	}

	private Store getStore(String email, String password)
			throws MessagingException {
		if (stores == null)
			stores = new ConcurrentLinkedHashMap.Builder<String, Store>()
					.maximumWeightedCapacity(100).build();
		Store store = null;
		if (stores.get(email) != null) {
			store = stores.get(email);
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
		stores.put(email, store);
		return store;
	}

	void retrieveSmsEntriesSince(UpdateInfo updateInfo,
			String email, String password, Date date) throws Exception {
		long then = System.currentTimeMillis();
		String query = "(incremental sms log retrieval)";
		ObjectType smsObjectType = ObjectType.getObjectType(connector(), "sms");
		String smsFolderName = guestService.getApiKeyAttribute(updateInfo.apiKey, "smsFolderName");
		try {
			Store store = getStore(email, password);
			Folder folder = store.getDefaultFolder();
			if (folder == null)
				throw new Exception("No default folder");
			folder = folder.getFolder(smsFolderName);
			if (folder == null)
				throw new Exception("No Sms Log");
			Message[] msgs = getMessagesInFolderSinceDate(folder, date);
			for (Message message : msgs) {
                date = message.getReceivedDate();
                flushEntry(updateInfo, email, message, SmsEntryFacet.class);
                updateStartDate(updateInfo,smsObjectType,date);
			}
			countSuccessfulApiCall(updateInfo.apiKey,
					smsObjectType.value(), then, query);
			return;
		} catch (Exception ex) {
            ex.printStackTrace();
			reportFailedApiCall(updateInfo.apiKey, smsObjectType.value(),
					then, query, Utils.stackTrace(ex), ex.getMessage());
			throw ex;
		}
	}

	void retrieveCallLogSinceDate(UpdateInfo updateInfo,
			String email, String password, Date date) throws Exception {
		long then = System.currentTimeMillis();
		String query = "(incremental call log retrieval)";
		ObjectType callLogObjectType = ObjectType.getObjectType(connector(),
				"call_log");
		String callLogFolderName = guestService.getApiKeyAttribute(updateInfo.apiKey, "callLogFolderName");
		try {
			Store store = getStore(email, password);
			Folder folder = store.getDefaultFolder();
			if (folder == null)
				throw new Exception("No default folder");
			folder = folder.getFolder(callLogFolderName);
			if (folder == null)
				throw new Exception("No Call Log");
			Message[] msgs = getMessagesInFolderSinceDate(folder, date);
			for (Message message : msgs) {
                date = message.getReceivedDate();
                flushEntry(updateInfo, email, message, CallLogEntryFacet.class);
                updateStartDate(updateInfo,callLogObjectType,date);
			}
			countSuccessfulApiCall(updateInfo.apiKey,
					callLogObjectType.value(), then, query);
			return;
		} catch (Exception ex) {
            ex.printStackTrace();
			reportFailedApiCall(updateInfo.apiKey,
					callLogObjectType.value(), then, query, Utils.stackTrace(ex),
                    ex.getMessage());
			throw ex;
		}
	}

	private Message[] getMessagesInFolder(Folder folder) throws Exception {
		if (!folder.isOpen())
			folder.open(Folder.READ_ONLY);
		Message[] msgs = folder.getMessages();

		FetchProfile fp = new FetchProfile();
		fp.add(FetchProfile.Item.ENVELOPE);
		fp.add(FetchProfile.Item.CONTENT_INFO);
		fp.add("Content");

		folder.fetch(msgs, fp);
		return msgs;
	}

	private Message[] getMessagesInFolderSinceDate(Folder folder, Date date)
			throws Exception {
		if (!folder.isOpen())
			folder.open(Folder.READ_ONLY);
		SentDateTerm term = new SentDateTerm(SentDateTerm.GT, date);
		Message[] msgs = folder.search(term);

		FetchProfile fp = new FetchProfile();
		fp.add(FetchProfile.Item.ENVELOPE);
		fp.add(FetchProfile.Item.CONTENT_INFO);
		fp.add("Content");

		folder.fetch(msgs, fp);
		return msgs;
	}

}
