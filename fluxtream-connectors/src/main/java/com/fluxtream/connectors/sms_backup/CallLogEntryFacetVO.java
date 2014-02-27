package com.fluxtream.connectors.sms_backup;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import org.joda.time.DateTime;

public class CallLogEntryFacetVO extends AbstractTimedFacetVO<CallLogEntryFacet>{

	public int secondsTalking;
	public String personName;
    public String personNumber;
    public String callType;
	
	@Override
	public void fromFacet(CallLogEntryFacet phoneCall, TimeInterval timeInterval, GuestSettings settings) {
        this.startMinute = toMinuteOfDay(phoneCall.date, timeInterval.getMainTimeZone());
        this.endMinute = this.startMinute + phoneCall.seconds/60;
        this.secondsTalking = Math.round(phoneCall.seconds);

        this.personName = phoneCall.personName;
        this.personNumber = phoneCall.personNumber;
        this.callType = phoneCall.callType.toString();

	}
}
