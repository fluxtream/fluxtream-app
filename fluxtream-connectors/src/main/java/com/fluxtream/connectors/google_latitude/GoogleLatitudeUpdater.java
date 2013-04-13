package com.fluxtream.connectors.google_latitude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.controllers.GoogleOAuth2Helper;
import com.fluxtream.connectors.updaters.AbstractGoogleOAuthUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.utils.Utils;
import com.google.api.client.googleapis.json.JsonCParser;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "Latitude", value = 2, objectTypes = { LocationFacet.class }, updateStrategyType = UpdateStrategyType.INCREMENTAL)
@JsonFacetCollection(LocationFacetVOCollection.class)
public class GoogleLatitudeUpdater extends AbstractGoogleOAuthUpdater {

	@Autowired
	GuestService guestService;

	@Autowired
	ApiDataService apiDataService;

    @Autowired
    GoogleOAuth2Helper oAuth2Helper;

    @Autowired
    JPADaoService jpaDaoService;

	public GoogleLatitudeUpdater() {
		super();
	}

	@Override
	public void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
		loadHistory(updateInfo, 0, System.currentTimeMillis());
	}

	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        final LocationFacet newest = jpaDaoService.findOne("google_latitude.location.newest", LocationFacet.class, updateInfo.getGuestId(), updateInfo.apiKey.getId());
        ApiUpdate lastSuccessfulUpdate = connectorUpdateService
				.getLastSuccessfulUpdate(updateInfo.apiKey);
		loadHistory(updateInfo, newest!=null?newest.start:lastSuccessfulUpdate.ts,
				System.currentTimeMillis());
	}

	private void loadHistory(UpdateInfo updateInfo, long from, long to)
			throws Exception {
        String accessToken = oAuth2Helper.getAccessToken(updateInfo.apiKey);
		HttpTransport transport = this.getTransport(updateInfo.apiKey);
		String key = env.get("google_latitudeApiKey");
		List<LocationFacet> locationList = executeList(updateInfo, transport,
				key, 1000, from, to, accessToken);
		if (locationList != null && locationList.size() > 0) {
			List<LocationFacet> storedLocations = new ArrayList<LocationFacet>();
			for (LocationFacet locationResource : locationList) {
				if (locationResource.timestampMs==0)
					continue;
                locationResource.guestId = updateInfo.getGuestId();
                locationResource.apiKeyId = updateInfo.apiKey.getId();
				locationResource.start = locationResource.timestampMs;
				locationResource.end = locationResource.timestampMs;
                locationResource.source = LocationFacet.Source.GOOGLE_LATITUDE;

				storedLocations.add(locationResource);
			}
            apiDataService.addGuestLocations(updateInfo.getGuestId(), storedLocations);
			Collections.sort(storedLocations);
			LocationFacet oldest = storedLocations.get(0);
            // Check if there is potentially a second or more of data left to get.  If so,
            // recurse with a new to time of a second before the oldest location we currently have.
            // Otherwise, end now
            if(oldest.timestampMs-1000 >= from) {
                loadHistory(updateInfo, from, oldest.timestampMs-1000);
            }
		}
	}

	private List<LocationFacet> executeList(UpdateInfo updateInfo,
			HttpTransport transport, String key, int maxResults, long minTime,
			long maxTime, String accessToken) throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "request url not set yet";
		try {
			transport.addParser(new JsonCParser());
			HttpRequest request = transport.buildGetRequest();
			LatitudeUrl latitudeUrl = LatitudeUrl.forLocation();
			latitudeUrl.maxResults = String.valueOf(maxResults);
			latitudeUrl.granularity = "best";
			latitudeUrl.minTime = String.valueOf(minTime);
			latitudeUrl.maxTime = String.valueOf(maxTime);
			latitudeUrl.put("location", "all");
            latitudeUrl.put("key", key);
            latitudeUrl.put("access_token", accessToken);
			request.url = latitudeUrl;
			requestUrl = latitudeUrl.build();
			HttpResponse response = request.execute();
			List<LocationFacet> result = response.parseAs(LocationList.class).items;
			countSuccessfulApiCall(updateInfo.apiKey,
					updateInfo.objectTypes, then, requestUrl);
			return result;
		} catch (Exception e) {
			countFailedApiCall(updateInfo.apiKey,
					updateInfo.objectTypes, then, requestUrl, Utils.stackTrace(e));
			throw e;
		}
	}

}
