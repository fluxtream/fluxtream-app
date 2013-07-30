package glacier.sms_backup;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.utils.SecurityUtils;

public class SmsEntryFacetVO extends AbstractInstantFacetVO<SmsEntryFacet> {

	public String personName;
	
	@Override
	public void fromFacet(SmsEntryFacet sms, TimeInterval timeInterval, GuestSettings settings) {
		this.startMinute = toMinuteOfDay(sms.dateReceived, timeInterval.getMainTimeZone());
		this.personName = sms.personName;
		if (SecurityUtils.isDemoUser())
			this.description = "***demo - text content hidden***";
		else
			this.description = sms.message;
	}
	
	protected String getSubtype(SmsEntryFacet sms) {
		return sms.type.toString().toLowerCase();
	}

}
