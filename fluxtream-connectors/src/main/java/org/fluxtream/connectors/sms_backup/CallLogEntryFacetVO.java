package org.fluxtream.connectors.sms_backup;

import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.domain.GuestSettings;

public class CallLogEntryFacetVO extends AbstractTimedFacetVO<CallLogEntryFacet>{

	public int secondsTalking;
	public String personName;
    public String personNumber;
    public String callType;
	
	@Override
	public void fromFacet(CallLogEntryFacet phoneCall, TimeInterval timeInterval, GuestSettings settings) {
        this.secondsTalking = Math.round(phoneCall.seconds);

        this.personName = phoneCall.personName;
        this.personNumber = phoneCall.personNumber;
        this.callType = phoneCall.callType.toString();

	}
}
