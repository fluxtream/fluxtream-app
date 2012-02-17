package com.fluxtream.connectors.withings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.domain.GuestSettings;

@SuppressWarnings("rawtypes")
public class WithingsFacetVOCollection extends AbstractFacetVOCollection {

	public List<WithingsBodyScaleMeasureFacetVO> weightMeasures;
	public List<WithingsBPMMeasureFacetVO> bpmMeasures;

	@Override
	public void extractFacets(List facets, TimeInterval timeInterval,
			GuestSettings settings) {
		if (facets.size() == 0)
			return;
		for (Object facet : facets) {
			if (facet instanceof WithingsBodyScaleMeasureFacet)
				addWeightMeasure((WithingsBodyScaleMeasureFacet) facet,
						timeInterval, settings);
			else if (facet instanceof WithingsBPMMeasureFacet)
				addBpmMeasure((WithingsBPMMeasureFacet) facet, timeInterval,
						settings);
		}
		interpolateWeightMeasures();
	}

	private void interpolateWeightMeasures() {
		sortWeightMeasures();
		List<WithingsBodyScaleMeasureFacetVO> interpolated = new ArrayList<WithingsBodyScaleMeasureFacetVO>();
		for (int i = 0; i < weightMeasures.size(); i++) {
			if ((i + 1) < weightMeasures.size()) {
				WithingsBodyScaleMeasureFacetVO thisMeasure = weightMeasures.get(i);
				WithingsBodyScaleMeasureFacetVO nextMeasure = weightMeasures.get(i+1);
				int nDays = nextMeasure.daysAgo-thisMeasure.daysAgo;
				if (nDays>1) {
					float weightDelta = thisMeasure.weight-nextMeasure.weight;
					float weightIncrement = weightDelta/nDays;
					for (int d=0; d<(nDays-1); d++) {
						WithingsBodyScaleMeasureFacetVO m = thisMeasure.copy();
						m.daysAgo = thisMeasure.daysAgo+(d+1);
						m.weight = thisMeasure.weight + ((d+1)*weightIncrement);
						interpolated.add(m);
					}
				}
			}
		}
		weightMeasures.addAll(interpolated);
		sortWeightMeasures();
	}

	private void sortWeightMeasures() {
		Collections.sort(weightMeasures,
				new Comparator<WithingsBodyScaleMeasureFacetVO>() {
					@Override
					public int compare(WithingsBodyScaleMeasureFacetVO m1,
							WithingsBodyScaleMeasureFacetVO m2) {
						return m1.daysAgo - m2.daysAgo;
					}
				});
	}

	private void addBpmMeasure(WithingsBPMMeasureFacet facet,
			TimeInterval timeInterval, GuestSettings settings) {
		if (bpmMeasures == null)
			bpmMeasures = new ArrayList<WithingsBPMMeasureFacetVO>();
		WithingsBPMMeasureFacetVO jsonFacet = new WithingsBPMMeasureFacetVO();
		jsonFacet.extractValues(facet, timeInterval, settings);
		bpmMeasures.add(jsonFacet);
	}

	private void addWeightMeasure(WithingsBodyScaleMeasureFacet facet,
			TimeInterval timeInterval, GuestSettings settings) {
		if (weightMeasures == null)
			weightMeasures = new ArrayList<WithingsBodyScaleMeasureFacetVO>();
		WithingsBodyScaleMeasureFacetVO jsonFacet = new WithingsBodyScaleMeasureFacetVO();
		jsonFacet.extractValues(facet, timeInterval, settings);
		weightMeasures.add(jsonFacet);
	}

}
