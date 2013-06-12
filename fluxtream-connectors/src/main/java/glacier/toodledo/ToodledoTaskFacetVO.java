package glacier.toodledo;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class ToodledoTaskFacetVO extends AbstractInstantFacetVO<ToodledoTaskFacet> {

	@Override
	protected void fromFacet(ToodledoTaskFacet facet,
			TimeInterval timeInterval, GuestSettings settings) {
		startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.getMainTimeZone());
		description = facet.title;
	}

}
