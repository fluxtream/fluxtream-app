package glacier.toodledo;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.domain.GuestSettings;

@SuppressWarnings("rawtypes")
public class ToodledoFacetVOCollection extends AbstractFacetVOCollection {

	List<ToodledoTaskFacetVO>  tasks;
	
	@Override
	public void extractFacets(List facets, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
		for (Object facet : facets) {
			if (facet instanceof ToodledoTaskFacet)
				addTask((ToodledoTaskFacet) facet, timeInterval, settings);
		}
	}

	private void addTask(ToodledoTaskFacet facet, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
		if (tasks==null) tasks = new ArrayList<ToodledoTaskFacetVO>();
		ToodledoTaskFacetVO jsonFacet = new ToodledoTaskFacetVO();
		jsonFacet.extractValues(facet, timeInterval, settings);
		tasks.add(jsonFacet);
	}

}
