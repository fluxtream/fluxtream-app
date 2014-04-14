package org.fluxtream.connectors.sms_backup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractPhotoFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DimensionModel;

public class SmsEntryFacetVO extends AbstractPhotoFacetVO<SmsEntryFacet> {

    private static transient int[] thumbnailSideLengths = new int[]{50,75,100,150,200,300};

	public String personName;
    public String personNumber;
    public String message;

    public String[] photoUrls;
    public String[] audioUrls;

    public Map<Integer, String> thumbnailUrls;
    public Map<Integer, DimensionModel> thumbnailSizes;

    public String smsType;
	
	@Override
	public void fromFacet(SmsEntryFacet sms, TimeInterval timeInterval, GuestSettings settings) {
		this.personName = sms.personName;
        this.smsType = sms.smsType.toString();
        this.personNumber = sms.personNumber;
        this.message = sms.message;
        UID = sms.getId();

        String homeBaseUrl = settings.config.get("homeBaseUrl");

        if (sms.hasAttachments){
            String[] fileNames = sms.attachmentNames.split(",");
            String[] mimeTypes = sms.attachmentMimeTypes.split(",");
            List<String> photoFiles = new LinkedList<String>();
            List<String> audioFiles = new LinkedList<String>();
            for (int i = 0; i < fileNames.length; i++){
                String fileUrl = String.format("%ssmsBackup/attachment/%s/%s",homeBaseUrl,sms.apiKeyId,fileNames[i]);
                if (mimeTypes[i].startsWith("image")){
                    photoFiles.add(fileUrl);
                }
                else if (mimeTypes[i].startsWith("audio")){
                    audioFiles.add(fileUrl);
                }
            }
            if (photoFiles.size() > 0){
                photoUrls = photoFiles.toArray(new String[]{});
                photoUrl = photoUrls[0];
                deviceName = "SMS_Backup";
                channelName = "sms";
                thumbnailUrls = new HashMap<Integer,String>();
                thumbnailSizes = new HashMap<Integer,DimensionModel>();
                for (int i = 0; i < thumbnailSideLengths.length; i++){
                    thumbnailUrls.put(i,photoUrl + "?s=" +  thumbnailSideLengths[i]);
                    thumbnailSizes.put(i,new DimensionModel(thumbnailSideLengths[i],thumbnailSideLengths[i]));
                }

            }
            if (audioFiles.size() > 0)
                audioUrls = audioFiles.toArray(new String[]{});
        }
	}

    @Override
    public String getPhotoUrl() {
        return this.photoUrl;
    }

    @Override
    public String getThumbnail(final int index) {
        return thumbnailUrls == null ? null : thumbnailUrls.get(index);
    }

    @Override
    public List<DimensionModel> getThumbnailSizes() {
        if (thumbnailSizes == null){
            return null;
        }
        else{
            List<DimensionModel> dimensionModels = new ArrayList<DimensionModel>();
            for (int index : thumbnailSizes.keySet()){
                dimensionModels.add(index,thumbnailSizes.get(index));
            }
            return dimensionModels;
        }

    }
}
