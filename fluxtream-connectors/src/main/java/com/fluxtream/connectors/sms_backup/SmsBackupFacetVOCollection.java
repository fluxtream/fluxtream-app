package com.fluxtream.connectors.sms_backup;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.domain.GuestSettings;

@SuppressWarnings("rawtypes")
public class SmsBackupFacetVOCollection extends AbstractFacetVOCollection {

	List<CallLogEntryFacetVO> calls;
	List<SmsEntryFacetVO> texts;
	int secondsTalking;
	
	@Override
	public void extractFacets(List facets, TimeInterval timeInterval, GuestSettings settings) {
		this.secondsTalking = 0;
		this.calls = new ArrayList<CallLogEntryFacetVO>();
		this.texts = new ArrayList<SmsEntryFacetVO>();
		for (Object facet: facets) {
			if (facet instanceof SmsEntryFacet) {
				SmsEntryFacetVO jsonFacet = new SmsEntryFacetVO();
				jsonFacet.extractValues((SmsEntryFacet)facet, timeInterval, settings);
				texts.add(jsonFacet);
			} else if (facet instanceof CallLogEntryFacet) {
				CallLogEntryFacetVO jsonFacet = new CallLogEntryFacetVO();
				CallLogEntryFacet callFacet = (CallLogEntryFacet)facet;
				jsonFacet.extractValues(callFacet, timeInterval, settings);
				secondsTalking+=callFacet.seconds;
				calls.add(jsonFacet);
			}
		}
	}

}
