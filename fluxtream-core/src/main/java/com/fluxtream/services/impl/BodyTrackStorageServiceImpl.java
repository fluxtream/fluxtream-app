package com.fluxtream.services.impl;

import java.util.HashMap;
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
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.HttpUtils;

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
	public void storeApiData(long guestId, List<AbstractFacet> facet) {

		Connector bodytrackConnector = Connector.getConnector("bodytrack");
		ApiKey bodytrackApiKey = guestService.getApiKey(guestId,
				bodytrackConnector);
		if (bodytrackApiKey == null)
			return;

		String user_id = guestService.getApiKeyAttribute(guestId,
				bodytrackConnector, "user_id");
		String host = guestService.getApiKeyAttribute(guestId,
				bodytrackConnector, "host");

		Map<String, String> params = new HashMap<String, String>();
		params.put("dev_nickname", "Fitbit");
		params.put(
				"data",
				"[[1313337599.9995,1776,1.0,5036],[1313423999.9995,1776,1.0,8109],[1313510399.9995,1776,1.0,7797]]");
		params.put("channel_names", "[\"caloriesOut\",\"MET\",\"steps\"]");

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

	@Override
	public void storeInitialHistory(long guestId, String connectorName) {
		TimeInterval timeInterval = new TimeInterval(0,
				System.currentTimeMillis(), TimeUnit.DAY, TimeZone.getDefault());
		List<AbstractFacet> facets = apiDataService.getApiDataFacets(guestId,
				Connector.getConnector(connectorName), null, timeInterval);
		storeApiData(guestId, facets);
	}

}
