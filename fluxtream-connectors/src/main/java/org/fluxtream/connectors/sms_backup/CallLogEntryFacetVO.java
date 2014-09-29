package org.fluxtream.connectors.sms_backup;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DurationModel;

public class CallLogEntryFacetVO extends AbstractTimedFacetVO<CallLogEntryFacet>{

	public DurationModel talkingTime;
	public String personName;
    public String personNumber;
    public String callType;
	
	@Override
	public void fromFacet(CallLogEntryFacet phoneCall, TimeInterval timeInterval, GuestSettings settings) {
        this.talkingTime = new DurationModel(Math.round(phoneCall.seconds));

        this.personName = phoneCall.personName;
        this.personNumber = phoneCall.personNumber;
        this.callType = phoneCall.callType.toString();

	}
}
