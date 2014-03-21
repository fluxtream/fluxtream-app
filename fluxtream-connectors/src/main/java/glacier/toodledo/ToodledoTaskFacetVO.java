package glacier.toodledo;

import java.util.Date;

import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.domain.GuestSettings;

public class ToodledoTaskFacetVO extends AbstractInstantFacetVO<ToodledoTaskFacet> {

	@Override
	protected void fromFacet(ToodledoTaskFacet facet,
			TimeInterval timeInterval, GuestSettings settings) {
		startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.getMainTimeZone());
		description = facet.title;
	}

}
