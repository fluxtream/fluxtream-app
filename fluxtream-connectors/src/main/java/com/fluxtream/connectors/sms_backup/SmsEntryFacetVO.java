package com.fluxtream.connectors.sms_backup;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.utils.SecurityUtils;
import org.joda.time.DateTime;

public class SmsEntryFacetVO extends AbstractInstantFacetVO<SmsEntryFacet> {

	public String personName;
    public String personNumber;
    public String message;

    public String smsType;
	
	@Override
	public void fromFacet(SmsEntryFacet sms, TimeInterval timeInterval, GuestSettings settings) {
		this.startMinute = new DateTime(sms.dateReceived).getMinuteOfDay();
		this.personName = sms.personName;
        this.smsType = sms.smsType.toString();
        this.personNumber = sms.personNumber;
        this.message = sms.message;
	}

}
