package com.fluxtream.services.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fluxtream.Configuration;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.HttpUtils;
import com.ibm.icu.util.StringTokenizer;

@Service
public class BodyTrackStorageServiceImpl implements BodyTrackStorageService {

	static Logger logger = Logger.getLogger(BodyTrackStorageServiceImpl.class);

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	MetadataService metadataService;

	@Override
	public void storeApiData(long guestId, List<AbstractFacet> facets) {

		Connector bodytrackConnector = Connector.getConnector("bodytrack");
		ApiKey bodytrackApiKey = guestService.getApiKey(guestId,
				bodytrackConnector);
		if (bodytrackApiKey == null)
			return;

		String user_id = guestService.getApiKeyAttribute(guestId,
				bodytrackConnector, "user_id");
		String host = guestService.getApiKeyAttribute(guestId,
				bodytrackConnector, "host");

		Map<String, List<AbstractFacet>> facetsByDeviceNickname = sortFacetsByDeviceNickname(facets);
		Iterator<String> eachDeviceName = facetsByDeviceNickname.keySet().iterator();
		while (eachDeviceName.hasNext()) {
			String deviceName = (String) eachDeviceName.next();
			storeDeviceData(guestId, user_id, host, facetsByDeviceNickname,
					deviceName);
		}

	}

	private void storeDeviceData(long guestId, String user_id, String host,
			Map<String, List<AbstractFacet>> facetsByDeviceNickname,
			String deviceName) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("dev_nickname", deviceName);
		Map<String, String> channelNamesMapping = getChannelNamesMapping(deviceName);
		params.put("channel_names", makeJSONArray(channelNamesMapping.values(), true));
		List<AbstractFacet> deviceFacets = facetsByDeviceNickname.get(deviceName);
		List<String> channelValues = extractChannelValuesFromFacets(
				channelNamesMapping, deviceFacets);
		params.put("data", makeJSONArray(channelValues, false));

		try {
			String result = HttpUtils.fetch("http://" + host + "/users/"
					+ user_id + "/upload", params, env);
			if (result.toLowerCase().startsWith("awesome")) {
				logger.info("Data successfully uploaded to BodyTrack: guestId: "
						+ guestId);
			} else {
				logger.warn("Could not upload data to BodyTrack data store: "
						+ result);
			}
		} catch (Exception e) {
			logger.warn("Could not upload data to BodyTrack data store: "
					+ e.getMessage());
		}
	}

	private List<String> extractChannelValuesFromFacets(
			Map<String, String> channelNamesMapping,
			List<AbstractFacet> deviceFacets) {
		List<String> channelValues = new ArrayList<String>();
		for (AbstractFacet deviceFacet : deviceFacets) {
			Iterator<String> eachFieldName = channelNamesMapping.keySet().iterator();
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			sb.append((long)((double)deviceFacet.start)/1000d);
			while (eachFieldName.hasNext()) {
				String fieldName = (String) eachFieldName.next();
				// TODO: handle special cases
				if (fieldName.startsWith("#"))
					continue;
				sb.append(",");
				Field field;
				try {
					field = deviceFacet.getClass().getField(fieldName);
					Class<?> type = field.getType();
					Object channelValue = field.get(deviceFacet);
					if (type==String.class) {
						sb.append("\"").append(channelValue).append("\"");
					} else
						sb.append(channelValue.toString());
				} catch (Exception e) {
					throw new RuntimeException("No such Field: " + fieldName);
				}
			}
			sb.append("]");
			channelValues.add(sb.toString());
		}
		return channelValues;
	}
	
	private String makeJSONArray(Collection<String> values, boolean addQuotes) {
		StringBuilder sb = new StringBuilder("[");
		Iterator<String> eachChannelName = values.iterator();
		for (int i=0; eachChannelName.hasNext(); i++) {
			String channelName = (String) eachChannelName.next();
			if (i>0) sb.append(",");
			if (addQuotes)
				sb.append("\"").append(channelName).append("\"");
			else
				sb.append(channelName);
		}
		sb.append("]");
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private Map<String,String> getChannelNamesMapping(String deviceName) {
		List<String> channelNamesMappings = (List<String>) env.bodytrackProperties.getProperty(deviceName);
		Map<String,String> mappings = new HashMap<String,String>();
		for (Iterator<String> iterator = channelNamesMappings.iterator(); iterator
				.hasNext();) {
			String mapping = (String) iterator.next();
			StringTokenizer tokenizer = new StringTokenizer(mapping, ":");
			String channelName = tokenizer.nextToken();
			String facetFieldName = tokenizer.nextToken();
			mappings.put(facetFieldName, channelName);
		}
		return mappings;
	}

	private Map<String, List<AbstractFacet>> sortFacetsByDeviceNickname(List<AbstractFacet> facets) {
		Map<String, List<AbstractFacet>> facetsByDeviceNickname = new HashMap<String, List<AbstractFacet>>();
		for (AbstractFacet facet : facets) {
			Connector connector = Connector.fromValue(facet.api);
			String connectorAndObjectType = connector.getName();
			if (connector.objectTypes()!=null&&connector.objectTypes().length>0) {
				connectorAndObjectType += "." + ObjectType.getObjectType(connector,
					facet.objectType).getName();
			}
			String deviceNickname = getDeviceNickname(connectorAndObjectType);
			if (deviceNickname==null) {
				logger.info("No Device Nickname for " + connectorAndObjectType);
				continue;
			}
			if (facetsByDeviceNickname.get(deviceNickname)==null)
				facetsByDeviceNickname.put(deviceNickname, new ArrayList<AbstractFacet>());
			facetsByDeviceNickname.get(deviceNickname).add(facet);
		}
		return facetsByDeviceNickname;
	}

	private String getDeviceNickname(String connectorAndObjectType) {
		Iterator<String> keys = env.bodytrackProperties.getKeys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			if (key.startsWith(connectorAndObjectType)) {
				if (key.endsWith("dev_nickname"))
					return (String) env.bodytrackProperties.getProperty(key);
			}
		}
		return null;
	}

	@Override
	public void storeInitialHistory(long guestId, String connectorName) {
		TimeInterval timeInterval = new TimeInterval(0,
				System.currentTimeMillis(), TimeUnit.DAY, TimeZone.getDefault());
		List<AbstractFacet> facets = apiDataService.getApiDataFacets(guestId,
				Connector.getConnector(connectorName), null, timeInterval);
		storeApiData(guestId, facets);
	}

}
