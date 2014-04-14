package glacier.toodledo;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.core.domain.GuestSettings;

public class ToodledoTaskFacetVO extends AbstractInstantFacetVO<ToodledoTaskFacet> {

	@Override
	protected void fromFacet(ToodledoTaskFacet facet,
			TimeInterval timeInterval, GuestSettings settings) {
		description = facet.title;
	}

}
