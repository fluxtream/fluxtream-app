package com.fluxtream.connectors.withings;

import static com.fluxtream.utils.HttpUtils.fetch;

import org.springframework.stereotype.Component;

import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiUpdate;

@Component
@Updater(prettyName = "Withings", value = 4, objectTypes = {
		WithingsBPMMeasureFacet.class, WithingsBodyScaleMeasureFacet.class }, extractor = WithingsFacetExtractor.class)
@JsonFacetCollection(WithingsFacetVOCollection.class)
public class WithingsUpdater extends AbstractUpdater {

	public WithingsUpdater() {
		super();
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws RateLimitReachedException, Exception {
		// get user info and find out first seen date
		long then = System.currentTimeMillis();
		String json = "";
		
		String url = "http://wbsapi.withings.net/measure?action=getmeas";
		url += "&userid="
				+ updateInfo.apiKey.getAttributeValue("userid", env);
		url += "&publickey="
				+ updateInfo.apiKey.getAttributeValue("publickey", env);
		url += "&startdate=0";
		url += "&enddate=" + System.currentTimeMillis() / 1000;

		try {
			json = fetch(url, env);
			countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, url);
		} catch (Exception e) {
			countFailedApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, url);
			throw e;
		}
		if (!json.equals(""))
			apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
	}

	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		long then = System.currentTimeMillis();
		String json = "";
		
		ApiUpdate lastSuccessfulUpdate = connectorUpdateService
				.getLastSuccessfulUpdate(updateInfo.apiKey.getGuestId(),
						connector());

		String url = "http://wbsapi.withings.net/measure?action=getmeas";
		url += "&userid=" + updateInfo.apiKey.getAttributeValue("userid", env);
		url += "&publickey="
				+ updateInfo.apiKey.getAttributeValue("publickey", env);
		url += "&startdate=" + lastSuccessfulUpdate.ts / 1000;
		url += "&enddate=" + System.currentTimeMillis() / 1000;
		
		try {
			json = fetch(url, env);
			countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, url);
		} catch (Exception e) {
			countFailedApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, url);
			throw e;
		}
		apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
	}

}
