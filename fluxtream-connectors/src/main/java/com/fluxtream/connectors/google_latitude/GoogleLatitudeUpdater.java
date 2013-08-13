package com.fluxtream.connectors.google_latitude;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.connectors.FileUploadSupport;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.location.LocationFacetVOCollection;
import com.fluxtream.connectors.updaters.AbstractGoogleOAuthUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ApiDataService;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "Latitude", value = 2, objectTypes = { LocationFacet.class }, updateStrategyType = UpdateStrategyType.INCREMENTAL)
@JsonFacetCollection(LocationFacetVOCollection.class)
public class GoogleLatitudeUpdater extends AbstractGoogleOAuthUpdater implements FileUploadSupport {

    @Autowired
    ApiDataService apiDataService;

	public GoogleLatitudeUpdater() {
		super();
	}

	@Override
	public void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
	}

	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
	}

    @Override
    public int importFile(final ApiKey apiKey, final File f) throws Exception {
        ZipFile zipFile = new ZipFile(f);
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.isDirectory()) continue;
            if (zipEntry.getName().endsWith("LocationHistory.json"))
                return parseLocations(apiKey, zipFile.getInputStream(zipEntry));
        }
        throw new RuntimeException("Couldn't find LocationHistory.json in the uploaded zip file");
    }

    private int parseLocations(final ApiKey apiKey, final InputStream inputStream) throws IOException {
        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createJsonParser(inputStream);

        JsonToken token;
        List<LocationFacet> locations = new ArrayList<LocationFacet>();

        getToLocationData(jParser);

        int nLocations = 0;
        while ((token = jParser.nextToken())!=null) {
            if (token==JsonToken.START_OBJECT) {
                jParser.nextToken();
                parseLocation(apiKey, jParser, locations);
                if (locations.size()==1000) {
                    nLocations += 1000;
                    if (apiKey!=null) apiDataService.addGuestLocations(apiKey.getGuestId(), locations);
                    locations.clear();
                }
            }
        }
        nLocations += locations.size();
        if (apiKey!=null) apiDataService.addGuestLocations(apiKey.getGuestId(), locations);
        return nLocations;
    }

    private void getToLocationData(final JsonParser jParser) throws IOException {
        // get to the first object
        while (jParser.nextToken()!=JsonToken.START_OBJECT);
        String currentName;
        while ((currentName=jParser.getCurrentName())==null)
            jParser.nextToken();
        // if it's one of the location fields, we're good
        if (Arrays.asList("timestampMs", "accuracy", "latitudeE7", "longitudeE7").contains(currentName))
            return;
        // else skip everything until we reach the "locations" array
        while (!jParser.getCurrentName().equals("locations"))
            jParser.nextToken();
        // this recursion should only happen once
        getToLocationData(jParser);
    }

    void parseLocation(final ApiKey apiKey, final JsonParser jParser, final List<LocationFacet> locations) throws IOException {
        LocationFacet locationFacet = new LocationFacet();
        if (apiKey!=null) {
            locationFacet.apiKeyId = apiKey.getId();
            locationFacet.guestId = apiKey.getGuestId();
        }
        locationFacet.timeUpdated = System.currentTimeMillis();
        locationFacet.source = LocationFacet.Source.GOOGLE_LATITUDE;

        do {
            String fieldName = jParser.getCurrentName();
            jParser.nextToken();
            if (fieldName.equals("timestampMs")) {
                long ts = Long.valueOf(jParser.getText());
                locationFacet.timestampMs = ts;
                locationFacet.start = ts;
                locationFacet.end = ts;
            } else if (fieldName.equals("accuracy")) {
                int accuracy = jParser.getIntValue();
                locationFacet.accuracy = accuracy;
            } else if (fieldName.equals("latitudeE7")) {
                int lat = jParser.getIntValue();
                locationFacet.latitude = lat/1E7f;
            } else if (fieldName.equals("longitudeE7")) {
                int lon = jParser.getIntValue();
                locationFacet.longitude = lon/1E7f;
            }
        } while (jParser.nextToken() != JsonToken.END_OBJECT);
        locations.add(locationFacet);
    }

}
