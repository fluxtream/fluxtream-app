package glacier.sms_backup;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;

public class CallLogEntryFacetVO extends AbstractTimedFacetVO<CallLogEntryFacet>{

	public int secondsTalking;
	public String personName;
	
	@Override
	public void fromFacet(CallLogEntryFacet phoneCall, TimeInterval timeInterval, GuestSettings settings) {
		String title = "";
		if (phoneCall.type==CallLogEntryFacet.CallType.INCOMING)
			title = phoneCall.personName;
		else
			title = phoneCall.personName;
		this.startMinute = toMinuteOfDay(phoneCall.date, timeInterval.timeZone);
		this.endMinute = this.startMinute + phoneCall.seconds/60;
		this.secondsTalking = Math.round(phoneCall.seconds);
		this.description = title;
		if (phoneCall.type==CallLogEntryFacet.CallType.MISSED)
			this.description += " missed call";
		else {
			if (this.secondsTalking==0)
				this.description += " missed call";
			else {
				int mins = this.secondsTalking/60;
				int secs = this.secondsTalking%60;
				this.description+=" ";
				if (mins>0) {
					this.description+= mins +" min";
					if (secs>0)
						this.description += ", " + secs + " s";
				}
				else this.description += secs + " s";
			}
		}
		this.personName = phoneCall.personName;
	}

	protected String getSubtype(CallLogEntryFacet phoneCall) {
		return phoneCall.type.toString().toLowerCase();
	}
}
