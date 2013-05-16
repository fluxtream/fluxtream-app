package com.fluxtream.connectors.withings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.stereotype.Component;

import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;

@Component
public class WithingsFacetExtractor extends AbstractFacetExtractor {

	private static final int WEIGHT = 1;
	private static final int HEIGHT = 4;
	private static final int FAT_FREE_MASS = 5;
	private static final int FAT_RATIO = 6;
	private static final int FAT_MASS_WEIGHT = 8;
	private static final int DIASTOLIC_BLOOD_PRESSURE = 9;
	private static final int SYSTOLIC_BLOOD_PRESSURE = 10;
	private static final int HEART_PULSE = 11;
			
	public List<AbstractFacet> extractFacets(ApiData apiData, ObjectType objectType) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
		JSONObject bodyScaleResponse = JSONObject.fromObject(apiData.json);
		
		if (!(bodyScaleResponse.has("status")) || bodyScaleResponse.getInt("status")!=0)
			return facets;
		JSONObject body = bodyScaleResponse.getJSONObject("body");
		JSONArray measuregrps = body.getJSONArray("measuregrps");
		if (measuregrps==null)
			return facets;
	
		@SuppressWarnings("rawtypes")
		Iterator iterator = measuregrps.iterator();
		while(iterator.hasNext()) {
			JSONObject measuregrp = (JSONObject) iterator.next();
			
			long date = measuregrp.getLong("date")*1000;
			JSONArray measures = measuregrp.getJSONArray ("measures");

			WithingsBodyScaleMeasureFacet facet = new WithingsBodyScaleMeasureFacet(apiData.updateInfo.apiKey.getId());
			facet.measureTime = date;
			facet.start = date;
			facet.end = date;
			super.extractCommonFacetData(facet, apiData);
			facet.objectType = ObjectType.getObjectType(connector(), "weight").value();
			
			@SuppressWarnings("rawtypes")
			Iterator measuresIterator = measures.iterator();
			boolean isBPM = false;
			while(measuresIterator.hasNext()) {
				JSONObject measure = (net.sf.json.JSONObject) measuresIterator.next();
				double pow = Math.abs (measure.getInt("unit"));
				double measureValue = measure.getDouble("value");
				double divisor = Math.pow (10, pow);
				switch(measure.getInt("type")) {
					case WEIGHT:
					float fValue = (float)(measureValue / divisor);
					facet.weight = fValue;
					break;
					case HEIGHT:
					facet.height = (float)(measureValue / divisor);
					break;
					case FAT_FREE_MASS:
					facet.fatFreeMass = (float)(measureValue / divisor);
					break;
					case FAT_RATIO:
					facet.fatRatio = (float)(measureValue / divisor);
					break;
					case FAT_MASS_WEIGHT:
					facet.fatMassWeight = (float)(measureValue / divisor);
					break;
					case DIASTOLIC_BLOOD_PRESSURE:
					isBPM = true;
					facet.diastolic = (float)(measureValue / divisor);
					break;
					case SYSTOLIC_BLOOD_PRESSURE:
					isBPM = true;
					facet.systolic = (float)(measureValue / divisor);
					break;
					case HEART_PULSE:
					isBPM = true;
					facet.heartPulse = (float)(measureValue / divisor);
					break;
				}
			}
			if (isBPM) {
                System.out.println("isBPM");
                if (objectType==ObjectType.getObjectType(connector(), "blood_pressure")) {
					WithingsBPMMeasureFacet bpmFacet = new WithingsBPMMeasureFacet(apiData.updateInfo.apiKey.getId());
					super.extractCommonFacetData(bpmFacet, apiData);
					bpmFacet.objectType = ObjectType.getObjectType(connector(), "blood_pressure").value();
					bpmFacet.measureTime = date;
					bpmFacet.start = date;
					bpmFacet.end = date;
					bpmFacet.systolic = facet.systolic;
					bpmFacet.diastolic = facet.diastolic;
					bpmFacet.heartPulse = facet.heartPulse;
					facets.add(bpmFacet);
				}
			} else {
                if (objectType==ObjectType.getObjectType(connector(), "weight"))
					facets.add(facet);
			}
		}
		
		return facets;
	}
	
	public static void main(String[] args) {
		int pow = 3;
		System.out.println("d= "  + Math.pow(10, pow));
	}
	
}
