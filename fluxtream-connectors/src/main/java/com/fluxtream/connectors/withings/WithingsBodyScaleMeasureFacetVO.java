package com.fluxtream.connectors.withings;

import java.io.Serializable;
import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.domain.GuestSettings.WeightMeasureUnit;
import com.fluxtream.utils.TimeUtils;

@SuppressWarnings("serial")
public class WithingsBodyScaleMeasureFacetVO extends
		AbstractInstantFacetVO<WithingsBodyScaleMeasureFacet> implements Serializable {

	public float weight;
	public String unitLabel;
	public int daysAgo = 0;

	public WithingsBodyScaleMeasureFacetVO copy() {
		WithingsBodyScaleMeasureFacetVO copy = new WithingsBodyScaleMeasureFacetVO();
		copy.weight = weight;
		copy.unitLabel = unitLabel;
		copy.daysAgo = daysAgo;
		copy.start = start;
		copy.startMinute = startMinute;
		return copy;
	}
	
	@Override
	public void fromFacet(WithingsBodyScaleMeasureFacet facet,
			TimeInterval timeInterval, GuestSettings settings) {
		long elapsed = timeInterval.start
				- TimeUtils.fromMidnight(facet.measureTime,
						timeInterval.timeZone);
		daysAgo = (int) (elapsed / (24 * 3600000));
		this.startMinute = toMinuteOfDay(new Date(facet.measureTime),
				timeInterval.timeZone);
		format(facet.weight, settings.weightMeasureUnit);
		this.description = new StringBuffer().append(weight).append(" ")
				.append(unitLabel).toString();
	}
	
	private void format(float weight, WeightMeasureUnit weightMeasureUnit) {
		switch (weightMeasureUnit) {
		case SI:
			this.weight = round(weight);
			this.unitLabel = "kg";
			break;
		case STONES:
			this.weight = round(weight * 0.157473044f);
			this.unitLabel = "stones";
			break;
		default:
			this.weight = round(weight * 2.20462262f);
			this.unitLabel = "lb";
		}
	}

	float round(float v) {
		return (float) Math.round(v * 100) / 100;
	}
}
